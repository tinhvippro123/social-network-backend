package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity(name = "CommunityGroup")
@Table(name = "community_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Group {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(length = 3000)
    private String description;
    private String avatar;
    private String coverImage;
    @Builder.Default
    private boolean publicGroup = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
