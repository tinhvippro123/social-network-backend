package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface GroupPostLikeRepository extends JpaRepository<GroupPostLike, String> {
    Optional<GroupPostLike> findByPostIdAndUserId(String postId, String userId);
    long countByPostId(String postId);
    boolean existsByPostIdAndUserId(String postId, String userId);
    void deleteByPostIdAndUserId(String postId, String userId);
    void deleteByPostId(String postId);

    @Modifying
    @Query("DELETE FROM GroupPostLike l WHERE l.post.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}
