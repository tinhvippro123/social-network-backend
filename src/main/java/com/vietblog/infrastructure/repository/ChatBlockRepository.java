package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.ChatBlock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatBlockRepository extends JpaRepository<ChatBlock, String> {
    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);
    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);

    @EntityGraph(attributePaths = {"blocked"})
    List<ChatBlock> findByBlockerId(String blockerId);
}
