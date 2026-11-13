package com.vietblog.application.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isFollowing")
    private boolean isFollowing;
    @JsonProperty("isMe")
    private boolean isMe;
    private String profileVisibility;
    private String allowMessages;
    private boolean canViewProfile;
    private boolean canMessage;
    private boolean emailVerified;
    private UserPreferencesDto preferences;

    public static UserProfileResponse fromEntity(User user, boolean isFollowing, boolean isMe, int accuratePostsCount) {
        return fromEntity(user, isFollowing, isMe, accuratePostsCount, true, !isMe);
    }

    public static UserProfileResponse fromEntity(
            User user,
            boolean isFollowing,
            boolean isMe,
            int accuratePostsCount,
            boolean canViewProfile,
            boolean canMessage) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(isMe ? user.getEmail() : null)
                .avatar(user.getAvatar())
                .bio(canViewProfile ? user.getBio() : null)
                .role(user.getRole() != null ? user.getRole().name().toLowerCase() : "user")
                .joinedAt(user.getJoinedAt())
                .postsCount(accuratePostsCount)
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .isFollowing(isFollowing)
                .isMe(isMe)
                .profileVisibility(user.effectiveProfileVisibility())
                .allowMessages(user.effectiveAllowMessages())
                .canViewProfile(canViewProfile)
                .canMessage(canMessage)
                .emailVerified(user.isEmailVerifiedEffective())
                .preferences(isMe ? UserPreferencesDto.fromEntity(user) : null)
                .build();
    }
}
