package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.post.CreatePostRequest;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.service.PostApplicationService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostApplicationService postApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreatePostRequest request) {
            
        PostResponse response = postApplicationService.createPost(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo bài viết thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "latest") String sort) {
            
        if ("trending".equalsIgnoreCase(sort)) {
            List<PostResponse> trending = postApplicationService.getTrendingPosts(limit);
            return ResponseEntity.ok(ApiResponse.success(trending, "Lấy danh sách thịnh hành thành công"));
        }

        Page<PostResponse> postPage = postApplicationService.getLatestPosts(page, limit);
        
        ApiResponse.Meta meta = new ApiResponse.Meta(page, limit, postPage.getTotalElements(), postPage.getTotalPages());
        ApiResponse<List<PostResponse>> response = ApiResponse.success(postPage.getContent(), "Lấy danh sách bài viết thành công");
        response.setMeta(meta);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable String id) {
        PostResponse response = postApplicationService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy bài viết thành công"));
    }
}
