package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, String> {
    List<GroupPostComment> findByPostIdOrderByCreatedAtAsc(String postId);
    long countByPostId(String postId);
    void deleteByPostId(String postId);

    @Modifying
    @Query("DELETE FROM GroupPostComment c WHERE c.post.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}
