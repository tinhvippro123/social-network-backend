package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.ChatMessage;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    @EntityGraph(attributePaths = {"sender", "replyTo", "replyTo.sender"})
    Page<ChatMessage> findByConversationId(String conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "replyTo", "replyTo.sender"})
    Page<ChatMessage> findByConversationIdAndRevokedFalseAndContentContainingIgnoreCase(String conversationId, String query, Pageable pageable);
    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDescIdDesc(String conversationId);

    @Query("select m from ChatMessage m where m.conversation.id in :conversationIds and m.createdAt = (select max(m2.createdAt) from ChatMessage m2 where m2.conversation.id = m.conversation.id)")
    List<ChatMessage> findLatestByConversationIds(@Param("conversationIds") Collection<String> conversationIds);

    Optional<ChatMessage> findByIdAndConversationId(String id, String conversationId);
    long countByConversationIdAndSenderIdNotAndRevokedFalse(String conversationId, String userId);
    long countByConversationIdAndSenderIdNotAndRevokedFalseAndCreatedAtGreaterThan(String conversationId, String userId, LocalDateTime readAt);

    @Query("select msg.conversation.id, count(msg) from ChatMessage msg, ConversationMember cm " +
           "where msg.conversation.id in :conversationIds " +
           "and cm.conversation.id = msg.conversation.id " +
           "and cm.user.id = :userId " +
           "and msg.sender.id <> :userId " +
           "and msg.revoked = false " +
           "and (cm.lastReadAt is null or msg.createdAt > cm.lastReadAt) " +
           "group by msg.conversation.id")
    List<Object[]> countUnreadByConversationIds(@Param("conversationIds") Collection<String> conversationIds, @Param("userId") String userId);

    default long countUnread(String conversationId, String userId, LocalDateTime readAt) {
        if (readAt == null) {
            return countByConversationIdAndSenderIdNotAndRevokedFalse(conversationId, userId);
        }
        return countByConversationIdAndSenderIdNotAndRevokedFalseAndCreatedAtGreaterThan(conversationId, userId, readAt);
    }
}
