package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.auth.JwtAuthenticationResponse;
import com.vietblog.application.dto.auth.LoginRequest;
import com.vietblog.application.dto.auth.RegisterRequest;
import com.vietblog.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.vietblog.application.service.UserApplicationService userApplicationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<com.vietblog.application.dto.user.UserProfileResponse>> getMyProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.vietblog.infrastructure.security.UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Vui lòng đăng nhập"));
        }
        com.vietblog.application.dto.user.UserProfileResponse response = userApplicationService.getCurrentUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin cá nhân thành công"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<com.vietblog.application.dto.user.UserProfileResponse>> updateMyProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.vietblog.infrastructure.security.UserPrincipal currentUser,
            @Valid @RequestBody com.vietblog.application.dto.user.UpdateProfileRequest request) {
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Vui lòng đăng nhập"));
        }
        com.vietblog.application.dto.user.UserProfileResponse response = userApplicationService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hồ sơ thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtAuthenticationResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "ÄÄƒng nháº­p thÃ nh cÃ´ng"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(null, "ÄÄƒng kÃ½ thÃ nh cÃ´ng"));
    }
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody com.vietblog.application.dto.auth.ChangePasswordRequest request,
            java.security.Principal principal) {
        
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "Vui lòng đăng nhập để thực hiện"));
        }

        authService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }
}
