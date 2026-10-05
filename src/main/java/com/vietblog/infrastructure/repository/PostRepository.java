package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {

    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);

    Page<Post> findByAuthorIdAndStatus(String authorId, Post.PostStatus status, Pageable pageable);

    Page<Post> findByCategoryIdAndStatus(String categoryId, Post.PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.trendingScore DESC")
    List<Post> findTrending(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    List<Post> findPostsWithLocation();

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Post> searchByKeyword(@Param("query") String query, Pageable pageable);

    long countByAuthorId(String authorId);
}
