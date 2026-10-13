package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupPost;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupPostRepository extends JpaRepository<GroupPost, String> {
    Page<GroupPost> findByGroupId(String groupId, Pageable pageable);
    Optional<GroupPost> findByIdAndGroupId(String id, String groupId);
    long countByGroupId(String groupId);
    void deleteByGroupId(String groupId);
}
