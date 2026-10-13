package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "chat_blocks", uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatBlock {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;
}
