package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.service.BookmarkService;
import com.vietblog.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @PathVariable String postId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        boolean isBookmarked = bookmarkService.toggleBookmark(postId, currentUser.getId());
        String message = isBookmarked ? "Đã lưu bài viết" : "Đã bỏ lưu bài viết";
        return ResponseEntity.ok(ApiResponse.success(isBookmarked, message));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkBookmark(
            @PathVariable String postId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        boolean isBookmarked = bookmarkService.checkIsBookmarked(postId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(isBookmarked, "Kiểm tra trạng thái lưu bài"));
    }
}
