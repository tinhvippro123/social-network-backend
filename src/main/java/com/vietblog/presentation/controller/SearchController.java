package com.vietblog.presentation.controller;

import java.util.Map;
import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.dto.search.*;
import com.vietblog.application.service.SearchApplicationService;
import com.vietblog.domain.search.TextAlgorithms;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {
    private final SearchApplicationService search;

    @GetMapping("/moderation/sentiment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<Map<String, Long>> sentimentStats() {
        return ApiResponse.success(search.sentimentStats(), "Comment sentiment counts");
    }

    @GetMapping("/search")
    public ApiResponse<List<PostResponse>> search(@RequestParam String q, @RequestParam(defaultValue = "keyword") String type,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(search.search(q, type, limit), "Search completed");
    }

    @GetMapping("/search/semantic")
    public ApiResponse<List<PostResponse>> semantic(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(search.search(q, "semantic", limit), "Search completed");
    }

    @PostMapping(value = "/search/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<PostResponse>> image(@RequestParam MultipartFile image, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(search.searchImage(image, limit), "Search completed");
    }

    @PostMapping(value = "/search/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<PostResponse>> document(@RequestParam MultipartFile file, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(search.searchDocument(file, limit), "Search completed");
    }

    @PostMapping(value = "/posts/{id}/search-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> index(@PathVariable String id, @RequestParam MultipartFile image, @AuthenticationPrincipal UserPrincipal user) {
        boolean admin = user.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        search.indexImage(id, user.getId(), admin, image);
        return ApiResponse.success(null, "Image indexed");
    }

    @GetMapping("/posts/nearby")
    public ApiResponse<List<PostResponse>> nearby(@RequestParam double lat, @RequestParam double lng,
                                                  @RequestParam(defaultValue = "5") double radius, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(search.nearby(lat, lng, radius, limit), "Nearby posts retrieved");
    }

    @GetMapping("/posts/{id}/summary")
    public ApiResponse<SummaryResponse> summary(@PathVariable String id, @RequestParam(defaultValue = "3") int sentences) {
        return ApiResponse.success(search.summary(id, sentences), "Summary generated");
    }

    @PostMapping("/nlp/sentiment")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TextAlgorithms.Sentiment> sentiment(@Valid @RequestBody TextRequest request) {
        return ApiResponse.success(TextAlgorithms.sentiment(request.text()), "Sentiment analyzed");
    }

    @GetMapping("/posts/{postId}/comments/{commentId}/sentiment")
    public ApiResponse<TextAlgorithms.Sentiment> commentSentiment(@PathVariable String postId, @PathVariable String commentId) {
        return ApiResponse.success(search.commentSentiment(postId, commentId), "Sentiment analyzed");
    }
}
