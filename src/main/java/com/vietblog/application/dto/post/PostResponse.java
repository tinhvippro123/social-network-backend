package com.vietblog.application.dto.post;

import com.vietblog.domain.entity.Post;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostResponse {
    private String id;
    private String title;
    private String excerpt;
    private String content;
    private String coverImage;
    private AuthorDto author;
    private CategoryDto category;
    private List<String> tags;
    private int viewsCount;
    private int upvotesCount;
    private int commentsCount;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    public static class AuthorDto {
        private String id;
        private String name;
        private String avatar;
    }
    
    @Data
    @Builder
    public static class CategoryDto {
        private String id;
        private String name;
        private String slug;
        private String icon;
    }

    public static PostResponse fromEntity(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .coverImage(post.getCoverImage())
                .author(AuthorDto.builder()
                        .id(post.getAuthor().getId())
                        .name(post.getAuthor().getName())
                        .avatar(post.getAuthor().getAvatar())
                        .build())
                .category(CategoryDto.builder()
                        .id(post.getCategory().getId())
                        .name(post.getCategory().getName())
                        .slug(post.getCategory().getSlug())
                        .icon(post.getCategory().getIcon())
                        .build())
                .tags(post.getTags())
                .viewsCount(post.getViewsCount())
                .upvotesCount(post.getUpvotesCount())
                .commentsCount(post.getCommentsCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
