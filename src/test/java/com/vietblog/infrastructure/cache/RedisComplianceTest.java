package com.vietblog.infrastructure.cache;

import com.vietblog.application.dto.category.CategoryResponse;
import com.vietblog.application.dto.category.CreateCategoryRequest;
import com.vietblog.application.port.CacheGateway;
import com.vietblog.application.service.CategoryApplicationService;
import com.vietblog.application.service.ChatEvent;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.service.CategoryDomainService;
import com.vietblog.infrastructure.messaging.ChatEventRelay;
import com.vietblog.infrastructure.messaging.ChatWebSocketHandler;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisComplianceTest {

    @Test
    @SuppressWarnings("unchecked")
    void resilientRedisCacheReadsWritesAndDegradesGracefullyWhenRedisOffline() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("vietblog:cache:test")).thenReturn("{\"ok\":true}");

        ResilientRedisCacheGateway cache = new ResilientRedisCacheGateway(redis);
        assertThat(cache.get("vietblog:cache:test")).contains("{\"ok\":true}");

        cache.put("vietblog:cache:test", "{\"ok\":true}", Duration.ofSeconds(60));
        verify(ops).set("vietblog:cache:test", "{\"ok\":true}", Duration.ofSeconds(60));

        cache.evict("vietblog:cache:test");
        verify(redis).delete("vietblog:cache:test");

        // Simulate Redis going offline
        when(ops.get("vietblog:cache:offline")).thenThrow(new RuntimeException("Redis connection refused"));
        assertThat(cache.get("vietblog:cache:offline")).isEmpty();
        // Circuit breaker prevents immediate retry spam while Redis is down
        assertThatCode(() -> cache.put("vietblog:cache:offline", "x", Duration.ofSeconds(10))).doesNotThrowAnyException();
    }

    @Test
    void categoryServiceCachesCategoriesInRedisAndEvictsOnUpdate() {
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        CategoryDomainService domainService = new CategoryDomainService();
        PostRepository postRepo = mock(PostRepository.class);
        CacheGateway cache = mock(CacheGateway.class);
        var json = JsonMapper.builder().build();

        Category cat = Category.builder().id("cat-1").name("Trí tuệ nhân tạo").slug("tri-tue-nhan-tao").build();
        when(categoryRepo.findAll()).thenReturn(List.of(cat));
        when(postRepo.countByCategoryIdAndStatus(eq("cat-1"), any())).thenReturn(5L);
        when(cache.get(CategoryApplicationService.CATEGORIES_CACHE_KEY)).thenReturn(Optional.empty());

        CategoryApplicationService service = new CategoryApplicationService(categoryRepo, domainService, postRepo, cache, json);
        List<CategoryResponse> firstLoad = service.getAllCategories();
        assertThat(firstLoad).hasSize(1);
        assertThat(firstLoad.get(0).getPostsCount()).isEqualTo(5);
        verify(cache).put(eq(CategoryApplicationService.CATEGORIES_CACHE_KEY), anyString(), any(Duration.class));

        // Second call hits Redis cache without touching DB
        String serialized = json.writeValueAsString(firstLoad);
        when(cache.get(CategoryApplicationService.CATEGORIES_CACHE_KEY)).thenReturn(Optional.of(serialized));
        List<CategoryResponse> cachedLoad = service.getAllCategories();
        assertThat(cachedLoad).hasSize(1);
        assertThat(cachedLoad.get(0).getSlug()).isEqualTo("tri-tue-nhan-tao");
        verify(categoryRepo, times(1)).findAll();

        // Mutation evicts cache
        CreateCategoryRequest createReq = new CreateCategoryRequest();
        createReq.setName("Lập trình Web");
        createReq.setDescription("Vue & Spring Boot");
        when(categoryRepo.existsByName("Lập trình Web")).thenReturn(false);
        when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.empty());
        when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> {
            Category saved = inv.getArgument(0);
            saved.setId("cat-2");
            return saved;
        });

        service.createCategory(createReq);
        verify(cache).evict(CategoryApplicationService.CATEGORIES_CACHE_KEY);
    }

    @Test
    void chatEventRelayAutoModeUsesRedisPubSubWhenOnlineAndFallsBackLocallyWhenOffline() {
        ChatWebSocketHandler sockets = mock(ChatWebSocketHandler.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        var json = JsonMapper.builder().build();
        ChatEvent event = new ChatEvent("message.created", "conv-1", "user-2", Map.of("text", "Xin chào"));

        ChatEventRelay relay = new ChatEventRelay(sockets, redis, json, "auto", () -> true);
        relay.publish(event);
        verify(redis).convertAndSend(eq(ChatEventRelay.CHANNEL), anyString());
        verify(sockets, never()).deliver(any());

        // When Redis Pub/Sub throws at runtime, falls back to local WebSocket delivery
        doThrow(new RuntimeException("Redis down")).when(redis).convertAndSend(eq(ChatEventRelay.CHANNEL), anyString());
        relay.publish(event);
        verify(sockets).deliver(event);
    }
}

