package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.community.CommunityDtos.*;
import com.vietblog.application.service.GroupService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/groups") @RequiredArgsConstructor
public class GroupController {
    private final GroupService service;
    @GetMapping
    public ApiResponse<List<GroupResponse>> list(@AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "false") boolean joined, @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        return paged(service.list(user.getId(), joined, q, page, limit));
    }
    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> get(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user) {
        return ok(service.get(id, user.getId()));
    }
    @PostMapping
    public ApiResponse<GroupResponse> create(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody GroupRequest request) {
        return ok(service.create(user.getId(), request));
    }
    @PutMapping("/{id}")
    public ApiResponse<GroupResponse> update(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody GroupRequest request) {
        return ok(service.update(id, user.getId(), request));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(id, user.getId()); return ok(null);
    }
    @PostMapping("/{id}/join")
    public ApiResponse<GroupResponse> join(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user) {
        return ok(service.join(id, user.getId()));
    }
    @DeleteMapping("/{id}/members/me")
    public ApiResponse<Void> leave(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user) {
        service.removeMember(id, user.getId(), user.getId()); return ok(null);
    }
    @GetMapping("/{id}/members")
    public ApiResponse<List<MemberResponse>> members(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        return paged(service.members(id, user.getId(), page, limit));
    }
    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody MemberRequest request) {
        service.addMember(id, user.getId(), request.userId()); return ok(null);
    }
    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable String id, @PathVariable String userId, @AuthenticationPrincipal UserPrincipal user) {
        service.removeMember(id, user.getId(), userId); return ok(null);
    }
    @PatchMapping("/{id}/members/{userId}/role")
    public ApiResponse<Void> role(@PathVariable String id, @PathVariable String userId, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody RoleRequest request) {
        service.setRole(id, user.getId(), userId, request.role()); return ok(null);
    }
    @GetMapping("/{id}/posts")
    public ApiResponse<List<GroupPostResponse>> posts(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        return paged(service.posts(id, user.getId(), page, limit));
    }
    @GetMapping("/{id}/posts/{postId}")
    public ApiResponse<GroupPostResponse> post(@PathVariable String id, @PathVariable String postId, @AuthenticationPrincipal UserPrincipal user) {
        return ok(service.post(id, postId, user.getId()));
    }
    @PostMapping("/{id}/posts")
    public ApiResponse<GroupPostResponse> createPost(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody GroupPostRequest request) {
        return ok(service.createPost(id, user.getId(), request));
    }
    @PutMapping("/{id}/posts/{postId}")
    public ApiResponse<GroupPostResponse> updatePost(@PathVariable String id, @PathVariable String postId, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody GroupPostRequest request) {
        return ok(service.updatePost(id, postId, user.getId(), request));
    }
    @DeleteMapping("/{id}/posts/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable String id, @PathVariable String postId, @AuthenticationPrincipal UserPrincipal user) {
        service.deletePost(id, postId, user.getId()); return ok(null);
    }
    @PostMapping("/{id}/posts/{postId}/like")
    public ApiResponse<GroupLikeResponse> toggleLike(@PathVariable String id, @PathVariable String postId, @AuthenticationPrincipal UserPrincipal user) {
        return ok(service.togglePostLike(id, postId, user.getId()));
    }
    @GetMapping("/{id}/posts/{postId}/comments")
    public ApiResponse<List<GroupCommentResponse>> comments(@PathVariable String id, @PathVariable String postId, @AuthenticationPrincipal UserPrincipal user) {
        return ok(service.getComments(id, postId, user != null ? user.getId() : null));
    }
    @PostMapping("/{id}/posts/{postId}/comments")
    public ApiResponse<GroupCommentResponse> addComment(@PathVariable String id, @PathVariable String postId, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody GroupCommentRequest request) {
        return ok(service.addComment(id, postId, user.getId(), request));
    }
    @DeleteMapping("/{id}/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable String id, @PathVariable String postId, @PathVariable String commentId, @AuthenticationPrincipal UserPrincipal user) {
        service.deleteComment(id, postId, commentId, user.getId()); return ok(null);
    }
    private static <T> ApiResponse<T> ok(T data) { return ApiResponse.success(data, "Success"); }
    private static <T> ApiResponse<List<T>> paged(Page<T> page) {
        ApiResponse<List<T>> response = ok(page.getContent());
        response.setMeta(new ApiResponse.Meta(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages()));
        return response;
    }
}
