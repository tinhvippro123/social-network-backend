package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Group;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, String> {
    @Query("select g from CommunityGroup g where (:joined = false or exists (select m.id from GroupMember m where m.group = g and m.user.id = :userId)) and (:query = '' or lower(g.name) like lower(concat('%', :query, '%')))")
    Page<Group> search(@Param("userId") String userId, @Param("joined") boolean joined, @Param("query") String query, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from CommunityGroup g where g.id = :id")
    Optional<Group> findLockedById(@Param("id") String id);
}
