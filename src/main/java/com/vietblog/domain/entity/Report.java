package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType type;

    private String targetTitle;

    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TargetType {
        POST, COMMENT, USER, GROUP
    }

    public enum ReportStatus {
        PENDING, RESOLVED, DISMISSED
    }
}
