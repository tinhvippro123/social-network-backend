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
    private int downvotesCount;
    private Double latitude;
    private Double longitude;
    private String locationAddress;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private Post.PostStatus status;
    private int commentsCount;
    private boolean bookmarked;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    public static class AuthorDto {
        private String id;
        private String name;
        private String avatar;
        private String bio;
        private int postsCount;
        private int followersCount;
    }
    
    @Data
    @Builder
    public static class CategoryDto {
        private String id;
        private String name;
        private String slug;
    }

    public static PostResponse fromEntity(Post post) {
        int count = post.getAuthor() != null ? post.getAuthor().getPostsCount() : 0;
        return fromEntity(post, count, false);
    }

    public static PostResponse fromEntity(Post post, int authorPostsCount) {
        return fromEntity(post, authorPostsCount, false);
    }

    public static PostResponse fromEntity(Post post, int authorPostsCount, boolean bookmarked) {
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
                        .bio(post.getAuthor().getBio())
                        .postsCount(authorPostsCount)
                        .followersCount(post.getAuthor().getFollowersCount())
                        .build())
                .category(CategoryDto.builder()
                        .id(post.getCategory().getId())
                        .name(post.getCategory().getName())
                        .slug(post.getCategory().getSlug())
                        .build())
                .tags(post.getTags())
                .viewsCount(post.getViewsCount())
                .upvotesCount(post.getUpvotesCount())
                .downvotesCount(post.getDownvotesCount())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .locationAddress(post.getLocationAddress())
                .eventStartTime(post.getEventStartTime())
                .eventEndTime(post.getEventEndTime())
                .status(post.getStatus())
                .commentsCount(post.getCommentsCount())
                .bookmarked(bookmarked)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
