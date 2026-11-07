package com.vietblog.application.dto.auth;

public record AuthSessionResult(
        String accessToken,
        String refreshToken,
        JwtAuthenticationResponse.UserDto user
) {}

