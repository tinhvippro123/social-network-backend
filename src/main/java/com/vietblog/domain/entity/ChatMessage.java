package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity @Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_conv_created", columnList = "conversation_id, createdAt DESC"),
    @Index(name = "idx_chat_messages_sender", columnList = "sender_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @Column(nullable = false) @Builder.Default
    private String type = "text";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;
    private String fileName;
    private String fileSize;
    @Column(length = 2000)
    private String fileUrl;
    @Column(length = 2000)
    private String imageUrl;
    private boolean revoked;
    private boolean pinned;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
}
