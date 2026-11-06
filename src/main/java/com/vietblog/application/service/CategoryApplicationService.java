package com.vietblog.application.service;

import com.vietblog.application.dto.category.CategoryResponse;
import com.vietblog.application.dto.category.CreateCategoryRequest;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.service.CategoryDomainService;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryApplicationService {

    private final CategoryRepository categoryRepository;
    private final CategoryDomainService categoryDomainService;
    private final PostRepository postRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .icon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon().trim() : "📁")
                .build();

        categoryDomainService.generateSlug(category);
        if (categoryRepository.findBySlug(category.getSlug()).isPresent()) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category savedCategory = categoryRepository.save(category);
        return toCategoryResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
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
        if (request.getIcon() != null && !request.getIcon().isBlank()) {
            category.setIcon(request.getIcon().trim());
        }

        Category updatedCategory = categoryRepository.save(category);
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
        category.setPostsCount(publishedCount);
        return CategoryResponse.fromEntity(category);
    }
}
