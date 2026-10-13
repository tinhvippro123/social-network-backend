package com.vietblog.presentation.controller;

import com.vietblog.application.dto.ApiResponse;
import com.vietblog.application.dto.report.ReportDtos;
import com.vietblog.application.service.ReportService;
import com.vietblog.domain.entity.Report;
import com.vietblog.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;

    @PostMapping("/api/reports")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportDtos.Response> create(@Valid @RequestBody ReportDtos.CreateRequest request,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(reports.create(currentUser.getId(), request), "Report submitted");
    }

    @GetMapping("/api/admin/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<List<ReportDtos.Response>> list(@RequestParam(required = false) Report.ReportStatus status,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int limit) {
        var result = reports.list(status, page, limit);
        var response = ApiResponse.success(result.getContent(), "Reports");
        response.setMeta(new ApiResponse.Meta(page, limit, result.getTotalElements(), result.getTotalPages()));
        return response;
    }

    @PatchMapping("/api/admin/reports/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<ReportDtos.Response> resolve(@PathVariable String id,
                                                   @Valid @RequestBody ReportDtos.ResolveRequest request,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(reports.resolve(id, currentUser.getId(), request.action()), "Report resolved");
    }

    @PatchMapping("/api/admin/reports/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ApiResponse<ReportDtos.Response> dismiss(@PathVariable String id,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(reports.dismiss(id, currentUser.getId()), "Report dismissed");
    }
}
