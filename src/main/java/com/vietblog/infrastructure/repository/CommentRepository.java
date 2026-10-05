package com.vietblog.infrastructure.repository;

import com.vietblog.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(String postId);
    long countByPostId(String postId);
}
