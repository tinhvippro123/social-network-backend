package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.admin.AdminDtos;
import com.vietblog.application.service.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ModerationController {
    private final ModerationService moderation;
    @PatchMapping("/posts/{id}/status")
    public ApiResponse<AdminDtos.PostResponse> status(@PathVariable String id,
            @Valid @RequestBody AdminDtos.PostStatusRequest request) {
        return ApiResponse.success(moderation.status(id, request.status()), "Post moderation updated");
    }
}
