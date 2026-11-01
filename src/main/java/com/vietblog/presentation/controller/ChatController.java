package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.chat.ChatDtos.*;
import com.vietblog.application.service.ChatService;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/chat") @RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> conversations(@AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        return paged(service.conversations(user.getId(), page, limit));
    }
    @PostMapping("/conversations")
    public ApiResponse<ConversationResponse> create(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody ConversationRequest request) {
        return ok(service.create(user.getId(), request));
    }
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageResponse>> messages(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int limit) {
        return paged(service.history(id, user.getId(), q, page, limit));
    }
    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<MessageResponse> send(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody SendRequest request) {
        return ok(service.send(id, user.getId(), request));
    }
    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> read(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user) {
        service.read(id, user.getId()); return ok(null);
    }
    @PatchMapping("/conversations/{id}/pin")
    public ApiResponse<Void> pin(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user, @RequestBody FlagRequest request) {
        service.preference(id, user.getId(), "pin", request.value()); return ok(null);
    }
    @PatchMapping("/conversations/{id}/mute")
    public ApiResponse<Void> mute(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user, @RequestBody FlagRequest request) {
        service.preference(id, user.getId(), "mute", request.value()); return ok(null);
    }
    @PatchMapping("/conversations/{id}/nickname")
    public ApiResponse<Void> updateNickname(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody NicknameRequest request) {
        service.setNickname(id, user.getId(), request.targetUserId(), request.nickname());
        return ok(null);
    }
    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> hide(@PathVariable String id, @AuthenticationPrincipal UserPrincipal user) {
        service.preference(id, user.getId(), "hide", true); return ok(null);
    }
    @PostMapping("/blocks/{userId}")
    public ApiResponse<Void> block(@PathVariable String userId, @AuthenticationPrincipal UserPrincipal user) {
        service.block(user.getId(), userId, true); return ok(null);
    }
    @DeleteMapping("/blocks/{userId}")
    public ApiResponse<Void> unblock(@PathVariable String userId, @AuthenticationPrincipal UserPrincipal user) {
        service.block(user.getId(), userId, false); return ok(null);
    }
    @DeleteMapping("/conversations/{id}/messages/{messageId}")
    public ApiResponse<MessageResponse> revoke(@PathVariable String id, @PathVariable String messageId, @AuthenticationPrincipal UserPrincipal user) {
        return ok(service.revoke(id, messageId, user.getId()));
    }
    @PatchMapping("/conversations/{id}/messages/{messageId}/pin")
    public ApiResponse<MessageResponse> pinMessage(@PathVariable String id, @PathVariable String messageId, @AuthenticationPrincipal UserPrincipal user, @RequestBody FlagRequest request) {
        return ok(service.pinMessage(id, messageId, user.getId(), request.value()));
    }
    private static <T> ApiResponse<T> ok(T data) { return ApiResponse.success(data, "Success"); }
    private static <T> ApiResponse<List<T>> paged(Page<T> page) {
        ApiResponse<List<T>> response = ok(page.getContent());
        response.setMeta(new ApiResponse.Meta(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages()));
        return response;
    }
}
