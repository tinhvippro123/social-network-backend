package com.vietblog.application.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vietblog.application.dto.user.UserPreferencesDto;
import com.vietblog.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtAuthenticationResponse {
    private String accessToken;
    private UserDto user;

    public JwtAuthenticationResponse(UserDto user) {
        this(null, user);
    }

    @Data
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String avatar;
        private String role;
        private String bio;
        private LocalDateTime joinedAt;
        private int postsCount;
        private int followersCount;
        private int followingCount;
        private boolean emailVerified;
        private UserPreferencesDto preferences;

        public static UserDto fromEntity(User user) {
            return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatar(),
                user.getRole().name(),
                user.getBio(),
                user.getJoinedAt(),
                user.getPostsCount(),
                user.getFollowersCount(),
                user.getFollowingCount(),
                user.isEmailVerifiedEffective(),
                UserPreferencesDto.fromEntity(user)
            );
        }
    }
}
