package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findLockedById(@Param("id") String id);

    @Query("select p from Post p where p.status = 'PUBLISHED' " +
           "and (:category is null or p.category.id = :category or p.category.slug = :category) " +
           "and (:query is null or lower(p.title) like lower(concat('%', :query, '%')) " +
           "or lower(p.content) like lower(concat('%', :query, '%')))")
    Page<Post> findFeed(@Param("category") String category, @Param("query") String query, Pageable pageable);

    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);

    Page<Post> findByAuthorIdAndStatus(String authorId, Post.PostStatus status, Pageable pageable);

    Page<Post> findByCategoryIdAndStatus(String categoryId, Post.PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.trendingScore DESC")
    List<Post> findTrending(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    List<Post> findPostsWithLocation();

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchByKeyword(@Param("query") String query, Pageable pageable);

    long countByAuthorId(String authorId);
}
