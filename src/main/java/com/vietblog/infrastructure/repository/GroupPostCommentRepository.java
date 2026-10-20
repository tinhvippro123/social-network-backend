package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, String> {
    List<GroupPostComment> findByPostIdOrderByCreatedAtAsc(String postId);
    long countByPostId(String postId);
    void deleteByPostId(String postId);
}
