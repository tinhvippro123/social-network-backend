package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    long countByPostId(String postId);
    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(String postId);
}
