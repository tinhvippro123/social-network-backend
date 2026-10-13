package com.vietblog.application.dto.report;

import com.vietblog.domain.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Locale;

public final class ReportDtos {
    private ReportDtos() { }

    public record CreateRequest(@NotBlank @Size(max = 36) String targetId,
                                @NotNull Report.TargetType type,
                                @NotBlank @Size(max = 255) String reason,
                                @Size(max = 2000) String description) { }
    public enum Action { KEEP, DELETE, BAN }
    public record ResolveRequest(@NotNull Action action) { }
    public record Reporter(String id, String name, String avatar) { }
    public record Response(String id, String targetId, String type, String targetTitle, String reason,
                           String description, String status, LocalDateTime createdAt, Reporter reporter,
                           String action, LocalDateTime resolvedAt, String resolvedById) {
        public static Response from(Report report) {
            return new Response(report.getId(), report.getTargetId(), report.getType().name().toLowerCase(Locale.ROOT),
                    report.getTargetTitle(), report.getReason(), report.getDescription(),
                    report.getStatus().name().toLowerCase(Locale.ROOT), report.getCreatedAt(),
                    new Reporter(report.getReporter().getId(), report.getReporter().getName(), report.getReporter().getAvatar()),
                    report.getResolutionAction(), report.getResolvedAt(),
                    report.getResolvedBy() == null ? null : report.getResolvedBy().getId());
        }
    }
}
