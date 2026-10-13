package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(length = 120)
    private String name;
    private boolean groupChat;
    @Column(unique = true, length = 80)
    private String directKey;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
