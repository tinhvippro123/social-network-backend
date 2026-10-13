package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.notification.NotificationResponse;
import com.vietblog.application.service.NotificationService;
import com.vietblog.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final NotificationService notifications;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@AuthenticationPrincipal UserPrincipal currentUser,
                                                       @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int limit) {
        var result = notifications.list(currentUser.getId(), unreadOnly, page, limit);
        var response = ApiResponse.success(result.getContent(), "Notifications");
        response.setMeta(new ApiResponse.Meta(page, limit, result.getTotalElements(), result.getTotalPages()));
        return response;
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(Map.of("count", notifications.unreadCount(currentUser.getId())), "Unread notification count");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Object> read(@PathVariable String id, @AuthenticationPrincipal UserPrincipal currentUser) {
        notifications.markRead(id, currentUser.getId());
        return ApiResponse.success(null, "Notification marked read");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> readAll(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(Map.of("updated", notifications.markAllRead(currentUser.getId())), "Notifications marked read");
    }
}
