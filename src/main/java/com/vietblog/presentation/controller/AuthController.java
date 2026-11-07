package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.auth.*;
import com.vietblog.application.dto.user.UpdateProfileRequest;
import com.vietblog.application.dto.user.UserProfileResponse;
import com.vietblog.application.service.AuthService;
import com.vietblog.application.service.UserApplicationService;
import com.vietblog.domain.exception.DomainException;
import com.vietblog.infrastructure.security.AuthCookieService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserApplicationService userApplicationService;
    private final AuthCookieService authCookieService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Vui lòng đăng nhập"));
        }
        UserProfileResponse response = userApplicationService.getCurrentUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin cá nhân thành công"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Vui lòng đăng nhập"));
        }
        UserProfileResponse response = userApplicationService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hồ sơ thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse) {
        AuthSessionResult session = authService.login(request);
        authCookieService.writeAuthCookies(servletResponse, session.accessToken(), session.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(new JwtAuthenticationResponse(session.user()), "Đăng nhập thành công"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng ký thành công"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Mã xác nhận đặt lại mật khẩu đã được tạo thành công"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String refreshToken = authCookieService.extractRefreshToken(servletRequest);
        if ((refreshToken == null || refreshToken.isBlank()) && request != null) {
            refreshToken = request.getToken();
        }
        try {
            AuthSessionResult session = authService.refreshToken(refreshToken);
            authCookieService.writeAuthCookies(servletResponse, session.accessToken(), session.refreshToken());
            return ResponseEntity.ok(ApiResponse.success(new JwtAuthenticationResponse(session.user()), "Làm mới phiên đăng nhập thành công"));
        } catch (DomainException ex) {
            authCookieService.clearAuthCookies(servletResponse);
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse servletResponse) {
        authCookieService.clearAuthCookies(servletResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "Vui lòng đăng nhập để thực hiện"));
        }

        authService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse servletResponse) {
        AuthSessionResult session = authService.loginWithGoogle(request);
        authCookieService.writeAuthCookies(servletResponse, session.accessToken(), session.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(new JwtAuthenticationResponse(session.user()), "Đăng nhập Google thành công"));
    }
}
