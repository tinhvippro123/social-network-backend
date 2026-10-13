package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupMember;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, String> {
    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);
    boolean existsByGroupIdAndUserId(String groupId, String userId);
    long countByGroupId(String groupId);
    Page<GroupMember> findByGroupId(String groupId, Pageable pageable);
    void deleteByGroupId(String groupId);
}
