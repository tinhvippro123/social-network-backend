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
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String categoryId;
    private String categoryName;
    private List<String> tags;
    private int viewsCount;
    private int upvotesCount;
    private int commentsCount;
    private LocalDateTime createdAt;

    public static PostResponse fromEntity(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .coverImage(post.getCoverImage())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getName())
                .authorAvatar(post.getAuthor().getAvatar())
                .categoryId(post.getCategory().getId())
                .categoryName(post.getCategory().getName())
                .tags(post.getTags())
                .viewsCount(post.getViewsCount())
                .upvotesCount(post.getUpvotesCount())
                .commentsCount(post.getCommentsCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
