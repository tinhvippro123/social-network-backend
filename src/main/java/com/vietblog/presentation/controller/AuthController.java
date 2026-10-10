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
                .body(ApiResponse.error("Vui lòng đăng nhập để thực hiện", "UNAUTHORIZED"));
        }

        try {
            authService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
        } catch (IllegalArgumentException e) {
            return org.springframework.http.ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }
}
