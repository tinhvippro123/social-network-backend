package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.dto.user.UpdateProfileRequest;
import com.vietblog.application.dto.user.UserProfileResponse;
import com.vietblog.application.dto.user.UserSummaryResponse;
import com.vietblog.application.service.UserApplicationService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserProfileResponse response = userApplicationService.getCurrentUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin cá nhân thành công"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userApplicationService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hồ sơ thành công"));
    }

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getRecommendedUsers(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "5") int limit) {
        String currentUserId = currentUser != null ? currentUser.getId() : null;
        List<UserSummaryResponse> response = userApplicationService.getRecommendedUsers(currentUserId, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách người dùng gợi ý thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String currentUserId = currentUser != null ? currentUser.getId() : null;
        UserProfileResponse response = userApplicationService.getUserProfile(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin hồ sơ thành công"));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<Boolean>> toggleFollow(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boolean isFollowing = userApplicationService.toggleFollow(id, currentUser.getId());
        String message = isFollowing ? "Đã theo dõi người dùng" : "Đã hủy theo dõi người dùng";
        return ResponseEntity.ok(ApiResponse.success(isFollowing, message));
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUserFollowers(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String currentUserId = currentUser != null ? currentUser.getId() : null;
        List<UserSummaryResponse> response = userApplicationService.getUserFollowers(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách người theo dõi thành công"));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUserFollowing(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String currentUserId = currentUser != null ? currentUser.getId() : null;
        List<UserSummaryResponse> response = userApplicationService.getUserFollowing(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách đang theo dõi thành công"));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getUserPosts(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<PostResponse> postPage = userApplicationService.getUserPosts(id, page, limit);

        ApiResponse.Meta meta = new ApiResponse.Meta(page, limit, postPage.getTotalElements(), postPage.getTotalPages());
        ApiResponse<List<PostResponse>> response = ApiResponse.success(postPage.getContent(), "Lấy danh sách bài viết thành công");
        response.setMeta(meta);

        return ResponseEntity.ok(response);
    }
}
