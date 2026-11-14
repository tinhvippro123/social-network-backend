package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.ConversationMember;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, String> {
    Optional<ConversationMember> findByConversationIdAndUserId(String conversationId, String userId);
    boolean existsByConversationIdAndUserId(String conversationId, String userId);

    @Query("select case when count(m) > 0 then true else false end from ConversationMember m where m.conversation.id = :conversationId and m.user.id = :userId and m.user.isActive = true")
    boolean existsActiveMember(@Param("conversationId") String conversationId, @Param("userId") String userId);

    @EntityGraph(attributePaths = {"user"})
    List<ConversationMember> findByConversationId(String conversationId);

    @EntityGraph(attributePaths = {"user", "conversation"})
    @Query("select m from ConversationMember m where m.conversation.id in :conversationIds")
    List<ConversationMember> findByConversationIdIn(@Param("conversationIds") Collection<String> conversationIds);

    @EntityGraph(attributePaths = {"conversation", "user"})
    Page<ConversationMember> findByUserIdAndHiddenFalse(String userId, Pageable pageable);
}
