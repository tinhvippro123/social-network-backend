package com.vietblog.infrastructure.config;

import com.vietblog.application.service.ChatEvent;
import com.vietblog.infrastructure.cache.RedisAvailabilityChecker;
import com.vietblog.infrastructure.messaging.ChatEventRelay;
import com.vietblog.infrastructure.messaging.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnExpression("'${chat.redis.enabled:auto}' != 'false'")
public class ChatRedisConfig {
    private static final Logger log = LoggerFactory.getLogger(ChatRedisConfig.class);

    @Bean
    public RedisMessageListenerContainer chatRedisListener(
            RedisConnectionFactory connectionFactory,
            ObjectMapper json,
            ChatWebSocketHandler sockets) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setAutoStartup(false);
        container.addMessageListener((message, pattern) -> {
            try {
                sockets.deliver(json.readValue(new String(message.getBody(), StandardCharsets.UTF_8), ChatEvent.class));
            } catch (RuntimeException ex) {
                log.warn("Discarded invalid chat relay event");
            }
        }, new ChannelTopic(ChatEventRelay.CHANNEL));
        return container;
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> chatRedisAutoStarter(
            RedisMessageListenerContainer chatRedisListener,
            RedisAvailabilityChecker redisAvailability,
            @Value("${chat.redis.enabled:auto}") String mode) {
        return event -> {
            if ("true".equalsIgnoreCase(mode) || redisAvailability.getAsBoolean()) {
                try {
                    if (!chatRedisListener.isRunning()) {
                        chatRedisListener.start();
                        log.info("Subscribed to Redis Pub/Sub channel '{}'.", ChatEventRelay.CHANNEL);
                    }
                } catch (RuntimeException ex) {
                    log.warn("Redis Pub/Sub listener deferred until Redis is reachable: {}", ex.getMessage());
                }
            }
        };
    }
}
