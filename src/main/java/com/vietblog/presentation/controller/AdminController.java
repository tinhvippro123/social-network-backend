package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.admin.AdminDtos;
import com.vietblog.application.service.AdminService;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService admin;

    @GetMapping("/stats")
    public ApiResponse<AdminDtos.Stats> stats() {
        return ApiResponse.success(admin.stats(), "Dashboard statistics");
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminDtos.UserResponse>> users(
            @RequestParam(required = false) User.Role role, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        var result = admin.users(role, search, page, limit);
        var response = ApiResponse.success(result.getContent(), "Users");
        response.setMeta(new ApiResponse.Meta(page, limit, result.getTotalElements(), result.getTotalPages()));
        return response;
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<AdminDtos.UserResponse> role(@PathVariable String id, @Valid @RequestBody AdminDtos.RoleRequest request,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(admin.changeRole(id, currentUser.getId(), request.role()), "User role updated");
    }

    @PatchMapping("/users/{id}/ban")
    public ApiResponse<AdminDtos.UserResponse> ban(@PathVariable String id, @Valid @RequestBody AdminDtos.BanRequest request,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(admin.setBanned(id, currentUser.getId(), request.banned()), "User access updated");
    }

    @GetMapping("/posts")
    public ApiResponse<List<AdminDtos.PostResponse>> posts(
            @RequestParam(required = false) Post.PostStatus status, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        var result = admin.posts(status, search, page, limit);
        var response = ApiResponse.success(result.getContent(), "Posts");
        response.setMeta(new ApiResponse.Meta(page, limit, result.getTotalElements(), result.getTotalPages()));
        return response;
    }

    @PatchMapping("/posts/{id}/status")
    public ApiResponse<AdminDtos.PostResponse> postStatus(@PathVariable String id,
                                                        @Valid @RequestBody AdminDtos.PostStatusRequest request) {
        return ApiResponse.success(admin.changePostStatus(id, request.status()), "Post status updated");
    }
}
