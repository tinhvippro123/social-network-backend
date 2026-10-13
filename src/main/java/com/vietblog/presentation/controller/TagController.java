package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tags;

    @GetMapping
    public ApiResponse<List<TagService.TagResponse>> list(@RequestParam(defaultValue = "") String search,
                                                        @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.success(tags.list(search, limit), "Published post tags");
    }
}
