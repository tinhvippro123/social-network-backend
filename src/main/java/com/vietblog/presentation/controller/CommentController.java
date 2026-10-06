package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.comment.CommentResponse;
import com.vietblog.application.dto.comment.CreateCommentRequest;
import com.vietblog.application.service.CommentApplicationService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentApplicationService commentApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable String postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        CommentResponse response = commentApplicationService.addComment(postId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đã thêm bình luận thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable String postId) {
        List<CommentResponse> comments = commentApplicationService.getCommentsByPostId(postId);
        return ResponseEntity.ok(ApiResponse.success(comments, "Lấy danh sách bình luận thành công"));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        commentApplicationService.deleteComment(commentId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa bình luận thành công"));
    }
}
