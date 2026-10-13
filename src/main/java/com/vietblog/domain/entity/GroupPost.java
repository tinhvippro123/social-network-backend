package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Group content is isolated from the public post feed, including private group content. */
@Entity @Table(name = "group_posts", indexes = @Index(columnList = "group_id,createdAt"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupPost {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id", nullable = false)
    private User author;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @Column(length = 1000)
    private String excerpt;
    private String coverImage;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}
