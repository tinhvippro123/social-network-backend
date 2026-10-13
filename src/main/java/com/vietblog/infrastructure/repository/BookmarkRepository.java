package com.vietblog.infrastructure.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    @Modifying
    @Query("delete from Bookmark b where b.post.id = :postId")
    void deleteForPost(@Param("postId") String postId);
    Page<Bookmark> findByUserIdAndPostStatusOrderByCreatedAtDesc(
            String userId, Post.PostStatus status, Pageable pageable);
    Optional<Bookmark> findByUserIdAndPostId(String userId, String postId);
    boolean existsByUserIdAndPostId(String userId, String postId);
    void deleteByUserIdAndPostId(String userId, String postId);
}
