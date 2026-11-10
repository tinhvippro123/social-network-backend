package com.vietblog.application.service;

import com.vietblog.application.dto.category.CategoryResponse;
import com.vietblog.application.dto.category.CreateCategoryRequest;
import com.vietblog.application.port.CacheGateway;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.service.CategoryDomainService;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryApplicationService {
    public static final String CATEGORIES_CACHE_KEY = "vietblog:cache:categories:all";
    private static final Duration CATEGORIES_TTL = Duration.ofMinutes(2);

    private final CategoryRepository categoryRepository;
    private final CategoryDomainService categoryDomainService;
    private final PostRepository postRepository;
    private final CacheGateway cache;
    private final ObjectMapper json;

    @Autowired
    public CategoryApplicationService(
            CategoryRepository categoryRepository,
            CategoryDomainService categoryDomainService,
            PostRepository postRepository,
            CacheGateway cache,
            ObjectMapper json) {
        this.categoryRepository = categoryRepository;
        this.categoryDomainService = categoryDomainService;
        this.postRepository = postRepository;
        this.cache = cache != null ? cache : CacheGateway.NOOP;
        this.json = json;
    }

    public CategoryApplicationService(
            CategoryRepository categoryRepository,
            CategoryDomainService categoryDomainService,
            PostRepository postRepository) {
        this(categoryRepository, categoryDomainService, postRepository, CacheGateway.NOOP, null);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .icon(request.getIcon() != null ? request.getIcon().trim() : "")
                .build();

        categoryDomainService.generateSlug(category);
        if (categoryRepository.findBySlug(category.getSlug()).isPresent()) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category savedCategory = categoryRepository.save(category);
        cache.evict(CATEGORIES_CACHE_KEY);
        return toCategoryResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        if (json != null) {
            Optional<String> cached = cache.get(CATEGORIES_CACHE_KEY);
            if (cached.isPresent()) {
                try {
                    return json.readValue(cached.get(), new TypeReference<List<CategoryResponse>>() {});
                } catch (RuntimeException ignored) {
                    cache.evict(CATEGORIES_CACHE_KEY);
                }
            }
        }

        List<CategoryResponse> responses = categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());

        if (json != null) {
            try {
                cache.put(CATEGORIES_CACHE_KEY, json.writeValueAsString(responses), CATEGORIES_TTL);
            } catch (RuntimeException ignored) {
                // fail open
            }
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String id) {
        Category category = resolveCategory(id);
        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(String id, CreateCategoryRequest request) {
        Category category = resolveCategory(id);
        String trimmedName = request.getName().trim();

        if (!category.getName().equals(trimmedName)) {
            if (categoryRepository.existsByName(trimmedName)) {
                throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            category.setName(trimmedName);
            categoryDomainService.generateSlug(category);
            categoryRepository.findBySlug(category.getSlug())
                    .filter(existing -> !existing.getId().equals(category.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
                    });
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon().trim());
        }

        Category updatedCategory = categoryRepository.save(category);
        cache.evict(CATEGORIES_CACHE_KEY);
        return toCategoryResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(String id) {
        Category category = resolveCategory(id);
        if (postRepository.countByCategoryId(category.getId()) > 0) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT,
                    "Không thể xóa danh mục đang có bài viết. Vui lòng chuyển các bài viết sang danh mục khác trước.");
        }
        categoryRepository.delete(category);
        cache.evict(CATEGORIES_CACHE_KEY);
    }

    private Category resolveCategory(String idOrSlug) {
        return categoryRepository.findById(idOrSlug)
                .or(() -> categoryRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private CategoryResponse toCategoryResponse(Category category) {
        int publishedCount = Math.toIntExact(
                postRepository.countByCategoryIdAndStatus(category.getId(), Post.PostStatus.PUBLISHED)
        );
        return CategoryResponse.fromEntity(category, publishedCount);
    }
}
