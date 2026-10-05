package com.vietblog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String coverImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    // Thá»‘ng kÃª
    @Builder.Default
    private int viewsCount = 0;
    @Builder.Default
    private int upvotesCount = 0;
    @Builder.Default
    private int downvotesCount = 0;
    @Builder.Default
    private int commentsCount = 0;

    // Vá»‹ trÃ­ Ä‘á»‹a lÃ½ (cho tÃ­nh nÄƒng Map + Geospatial Search)
    private Double latitude;
    private Double longitude;
    private String locationAddress;

    // Sá»± kiá»‡n (cho bÃ i viáº¿t dáº¡ng event)
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;

    // â”€â”€ Trending Score (Ä‘Æ°á»£c tÃ­nh bá»Ÿi giáº£i thuáº­t) â”€â”€
    @Builder.Default
    private double trendingScore = 0.0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PostStatus {
        DRAFT, PUBLISHED, HIDDEN
    }
}
