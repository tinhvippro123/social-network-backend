package com.vietblog.application.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vietblog.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private String id;
    private String name;
    private String avatar;
    private String bio;
    private String role;
    private int postsCount;
    private int followersCount;
    @JsonProperty("isFollowing")
    private boolean isFollowing;

    public static UserSummaryResponse fromEntity(User user, boolean isFollowing) {
        return fromEntity(user, isFollowing, user.getPostsCount());
    }

    public static UserSummaryResponse fromEntity(User user, boolean isFollowing, int postsCount) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole() != null ? user.getRole().name().toLowerCase() : "user")
                .postsCount(postsCount)
                .followersCount(user.getFollowersCount())
                .isFollowing(isFollowing)
                .build();
    }
}
