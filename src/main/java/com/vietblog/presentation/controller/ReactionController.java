package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.reaction.ReactionRequest;
import com.vietblog.application.service.ReactionService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @PathVariable String postId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        reactionService.toggleReaction(postId, currentUser.getId(), request.getEmoji());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật thả cảm xúc"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getReactionSummary(@PathVariable String postId) {
        Map<String, Long> summary = reactionService.getReactionSummary(postId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy thống kê cảm xúc thành công"));
    }
}
