package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {
    private final SystemSettingsService settings;

    @GetMapping
    public ApiResponse<Map<String, Object>> get() {
        return ApiResponse.success(settings.getAll(), "System settings");
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> update(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(settings.update(body), "System settings updated");
    }
}
