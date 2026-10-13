package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.ChatMessage;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    Page<ChatMessage> findByConversationId(String conversationId, Pageable pageable);
    Page<ChatMessage> findByConversationIdAndRevokedFalseAndContentContainingIgnoreCase(String conversationId, String query, Pageable pageable);
    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDescIdDesc(String conversationId);
    Optional<ChatMessage> findByIdAndConversationId(String id, String conversationId);
    long countByConversationIdAndSenderIdNotAndRevokedFalse(String conversationId, String userId);
    long countByConversationIdAndSenderIdNotAndRevokedFalseAndCreatedAtGreaterThan(String conversationId, String userId, LocalDateTime readAt);

    default long countUnread(String conversationId, String userId, LocalDateTime readAt) {
        if (readAt == null) {
            return countByConversationIdAndSenderIdNotAndRevokedFalse(conversationId, userId);
        }
        return countByConversationIdAndSenderIdNotAndRevokedFalseAndCreatedAtGreaterThan(conversationId, userId, readAt);
    }
}
