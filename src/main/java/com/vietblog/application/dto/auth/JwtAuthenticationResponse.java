package com.vietblog.application.dto.auth;

import com.vietblog.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private UserDto user;

    @Data
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String avatar;
        private String role;
        
        public static UserDto fromEntity(User user) {
            return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatar(),
                user.getRole().name()
            );
        }
    }
}
