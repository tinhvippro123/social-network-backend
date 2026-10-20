package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.GroupPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupPostLikeRepository extends JpaRepository<GroupPostLike, String> {
    Optional<GroupPostLike> findByPostIdAndUserId(String postId, String userId);
    long countByPostId(String postId);
    boolean existsByPostIdAndUserId(String postId, String userId);
    void deleteByPostIdAndUserId(String postId, String userId);
    void deleteByPostId(String postId);
}
