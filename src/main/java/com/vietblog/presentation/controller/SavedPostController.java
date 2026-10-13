package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.service.BookmarkService;
import com.vietblog.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmarks")
public class SavedPostController {
    private final BookmarkService bookmarks;

    @GetMapping
    public ApiResponse<List<PostResponse>> list(@AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        var results = bookmarks.list(user.getId(), page, limit);
        var response = ApiResponse.success(results.getContent(), "Saved posts retrieved");
        response.setMeta(new ApiResponse.Meta(page, limit, results.getTotalElements(), results.getTotalPages()));
        return response;
    }
}
