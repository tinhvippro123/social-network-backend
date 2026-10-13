package com.vietblog.application.service;

import com.vietblog.application.dto.category.CategoryResponse;
import com.vietblog.application.dto.category.CreateCategoryRequest;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.service.CategoryDomainService;
import com.vietblog.infrastructure.repository.CategoryRepository;
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

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Kiểm tra xem tên danh mục đã tồn tại chưa
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        // Uỷ quyền cho Domain Service tạo Slug
        categoryDomainService.generateSlug(category);

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional
    public CategoryResponse updateCategory(String id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        // Nếu đổi tên, phải check trùng lặp và tạo lại slug
        if (!category.getName().equals(request.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BusinessRuleException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            category.setName(request.getName());
            categoryDomainService.generateSlug(category);
        }

        category.setDescription(request.getDescription());
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updatedCategory);
    }

    @Transactional
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        // Lưu ý: Thực tế cần kiểm tra xem danh mục có bài viết nào không trước khi xóa.
        // Tạm thời cho phép xóa trực tiếp.
        categoryRepository.deleteById(id);
    }
}
