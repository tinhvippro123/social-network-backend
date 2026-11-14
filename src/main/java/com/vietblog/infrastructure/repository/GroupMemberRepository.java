package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupMember;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, String> {
    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);
    boolean existsByGroupIdAndUserId(String groupId, String userId);
    long countByGroupId(String groupId);
    @EntityGraph(attributePaths = {"user"})
    Page<GroupMember> findByGroupId(String groupId, Pageable pageable);
    void deleteByGroupId(String groupId);

    @Query("select m.group.id, count(m) from GroupMember m where m.group.id in :groupIds group by m.group.id")
    List<Object[]> countByGroupIds(@Param("groupIds") Collection<String> groupIds);

    @Query("select m from GroupMember m where m.group.id in :groupIds and m.user.id = :userId")
    List<GroupMember> findByGroupIdInAndUserId(@Param("groupIds") Collection<String> groupIds, @Param("userId") String userId);
}
