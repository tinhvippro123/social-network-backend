package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.service.ReactionService;
import com.vietblog.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}")
public class VoteController {
    private final ReactionService reactions;

    @PostMapping("/upvote")
    public ApiResponse<Void> upvote(@PathVariable String postId, @AuthenticationPrincipal UserPrincipal user) {
        reactions.toggleReaction(postId, user.getId(), "UPVOTE");
        return ApiResponse.success(null, "Vote updated");
    }

    @PostMapping("/downvote")
    public ApiResponse<Void> downvote(@PathVariable String postId, @AuthenticationPrincipal UserPrincipal user) {
        reactions.toggleReaction(postId, user.getId(), "DOWNVOTE");
        return ApiResponse.success(null, "Vote updated");
    }
}
