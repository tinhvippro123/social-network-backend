package com.vietblog.application.dto.admin;

import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public final class AdminDtos {
    private AdminDtos() { }

    public record RoleRequest(@NotNull User.Role role) { }
    public record BanRequest(@NotNull Boolean banned) { }
    public record PostStatusRequest(@NotNull Post.PostStatus status) { }

    public record UserResponse(String id, String name, String email, String avatar, String role,
                               boolean active, LocalDateTime joinedAt, LocalDateTime lastLoginAt, long postsCount) {
        public static UserResponse from(User user) {
            return from(user, 0);
        }

        public static UserResponse from(User user, long postsCount) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getAvatar(),
                    user.getRole().name().toLowerCase(Locale.ROOT), user.isActive(), user.getJoinedAt(),
                    user.getLastLoginAt(), postsCount);
        }
    }

    public record PostResponse(String id, String title, String status, String authorId, String authorName,
                               String categoryId, String categoryName, int viewsCount, LocalDateTime createdAt,
                               String coverImage, String authorAvatar, String categoryIcon,
                               int upvotesCount, int commentsCount, long reportsCount) {
        public static PostResponse from(Post post) {
            return from(post, 0L);
        }

        public static PostResponse from(Post post, long reportsCount) {
            return new PostResponse(post.getId(), post.getTitle(), post.getStatus().name().toLowerCase(Locale.ROOT),
                    post.getAuthor().getId(), post.getAuthor().getName(), post.getCategory().getId(),
                    post.getCategory().getName(), post.getViewsCount(), post.getCreatedAt(),
                    post.getCoverImage(), post.getAuthor().getAvatar(), post.getCategory().getIcon(),
                    post.getUpvotesCount(), post.getCommentsCount(), reportsCount);
        }
    }

    public record DailyGrowth(LocalDate date, long users, long posts) { }
    public record CategoryStat(String name, String icon, long count, int pct) { }
    /** type: post | user | report — time là thời điểm thật của sự kiện */
    public record Activity(String type, String text, LocalDateTime time) { }
    public record Stats(long totalUsers, long newUsersToday, long totalPosts, long newPostsToday,
                        long totalComments, long totalGroups, long totalReports, long pendingReports, long totalViews,
                        List<DailyGrowth> growth, long newUsersInRange, long newPostsInRange,
                        List<CategoryStat> topCategories, List<Activity> recentActivities) { }
}
