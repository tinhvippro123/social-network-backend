package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.ConversationMember;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, String> {
    Optional<ConversationMember> findByConversationIdAndUserId(String conversationId, String userId);
    boolean existsByConversationIdAndUserId(String conversationId, String userId);
    List<ConversationMember> findByConversationId(String conversationId);
    Page<ConversationMember> findByUserIdAndHiddenFalse(String userId, Pageable pageable);
}
