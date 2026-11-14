package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupPost;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupPostRepository extends JpaRepository<GroupPost, String> {
    @EntityGraph(attributePaths = {"author", "group"})
    Page<GroupPost> findByGroupId(String groupId, Pageable pageable);
    Optional<GroupPost> findByIdAndGroupId(String id, String groupId);
    long countByGroupId(String groupId);
    void deleteByGroupId(String groupId);

    @Query("select p.group.id, count(p) from GroupPost p where p.group.id in :groupIds group by p.group.id")
    List<Object[]> countByGroupIds(@Param("groupIds") Collection<String> groupIds);
}
