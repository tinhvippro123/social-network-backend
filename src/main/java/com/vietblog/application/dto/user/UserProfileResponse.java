package com.vietblog.application.dto.user;

import com.vietblog.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String name;
    private String email;
    private String avatar;
    private String bio;
    private String role;
    private LocalDateTime joinedAt;
    private int postsCount;
    private int followersCount;
    private int followingCount;
    private boolean isFollowing;
    private boolean isMe;

    public static UserProfileResponse fromEntity(User user, boolean isFollowing, boolean isMe, int accuratePostsCount) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(isMe ? user.getEmail() : null)
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole() != null ? user.getRole().name().toLowerCase() : "user")
                .joinedAt(user.getJoinedAt())
                .postsCount(accuratePostsCount)
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .isFollowing(isFollowing)
                .isMe(isMe)
                .build();
    }
}
