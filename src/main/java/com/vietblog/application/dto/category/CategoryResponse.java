package com.vietblog.application.dto.category;

import com.vietblog.domain.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private int postsCount;
    private LocalDateTime createdAt;

    public static CategoryResponse fromEntity(Category category) {
        return fromEntity(category, category.getPostsCount());
    }

    public static CategoryResponse fromEntity(Category category, int postsCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .postsCount(postsCount)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
