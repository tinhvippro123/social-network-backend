package com.vietblog.infrastructure.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.vietblog.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    @Modifying
    @Query("update Comment c set c.parent = null where c.post.id = :postId")
    void detachForPost(@Param("postId") String postId);
    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteForPost(@Param("postId") String postId);
    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(String postId);
    long countByPostId(String postId);
}
