package com.vietblog.application.dto.category;

import com.vietblog.domain.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String description;
    private LocalDateTime createdAt;

    public static CategoryResponse fromEntity(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
