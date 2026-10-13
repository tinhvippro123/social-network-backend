package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.service.TagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {
    private final TagService tags;
    public record RenameRequest(@NotBlank @Size(max = 64) String name) { }

    @PatchMapping("/{name}")
    public ApiResponse<Integer> rename(@PathVariable String name, @Valid @RequestBody RenameRequest request) {
        return ApiResponse.success(tags.rename(name, request.name()), "Tag assignments updated");
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Integer> delete(@PathVariable String name) {
        return ApiResponse.success(tags.delete(name), "Tag assignments removed");
    }
}
