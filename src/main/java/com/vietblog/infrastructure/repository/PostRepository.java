package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findLockedById(@Param("id") String id);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where p.status = 'PUBLISHED' and (p.category.id = :category or p.category.slug = :category)")
    Page<Post> findByCategory(@Param("category") String category, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where p.status = 'PUBLISHED' and (lower(p.title) like :pattern or lower(p.content) like :pattern)")
    Page<Post> findByKeyword(@Param("pattern") String pattern, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where p.status = 'PUBLISHED' and (p.category.id = :category or p.category.slug = :category) and (lower(p.title) like :pattern or lower(p.content) like :pattern)")
    Page<Post> findByCategoryAndKeyword(@Param("category") String category, @Param("pattern") String pattern, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select distinct p from Post p join p.tags t where p.status = 'PUBLISHED' and lower(t) = lower(:tag)")
    Page<Post> findByTag(@Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select distinct p from Post p join p.tags t where p.status = 'PUBLISHED' and (p.category.id = :category or p.category.slug = :category) and lower(t) = lower(:tag)")
    Page<Post> findByCategoryAndTag(@Param("category") String category, @Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select distinct p from Post p join p.tags t where p.status = 'PUBLISHED' and (lower(p.title) like :pattern or lower(p.content) like :pattern) and lower(t) = lower(:tag)")
    Page<Post> findByKeywordAndTag(@Param("pattern") String pattern, @Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select distinct p from Post p join p.tags t where p.status = 'PUBLISHED' and (p.category.id = :category or p.category.slug = :category) and (lower(p.title) like :pattern or lower(p.content) like :pattern) and lower(t) = lower(:tag)")
    Page<Post> findByCategoryAndKeywordAndTag(@Param("category") String category, @Param("pattern") String pattern, @Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findByAuthorIdAndStatus(String authorId, Post.PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findByCategoryIdAndStatus(String categoryId, Post.PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.trendingScore DESC")
    List<Post> findTrending(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("select p from Post p where p.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED and p.createdAt > :threshold")
    List<Post> findRecentPublishedWithAuthor(@Param("threshold") LocalDateTime threshold);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where p.id in :ids")
    List<Post> findAllWithDetailsByIdIn(@Param("ids") Collection<String> ids);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    List<Post> findPostsWithLocation();

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Post> searchByKeyword(@Param("query") String query, Pageable pageable);

    long countByAuthorId(String authorId);

    long countByAuthorIdAndStatus(String authorId, Post.PostStatus status);

    @Query("select p.author.id, count(p) from Post p where p.author.id in :authorIds and p.status = :status group by p.author.id")
    List<Object[]> countByAuthorIdsAndStatus(@Param("authorIds") Collection<String> authorIds, @Param("status") Post.PostStatus status);

    long countByCategoryId(String categoryId);

    long countByCategoryIdAndStatus(String categoryId, Post.PostStatus status);

    @Query("select p.category.id, count(p) from Post p where p.category.id in :categoryIds and p.status = :status group by p.category.id")
    List<Object[]> countByCategoryIdsAndStatus(@Param("categoryIds") Collection<String> categoryIds, @Param("status") Post.PostStatus status);

    @Query("select p.id from Post p where p.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED " +
           "and p.coverImage is not null and trim(p.coverImage) <> '' " +
           "and not exists (select 1 from PostImageFeatures f where f.postId = p.id)")
    List<String> findPublishedIdsWithUnindexedCoverImage();

    @Query("select p.author.id, count(p) from Post p where p.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED group by p.author.id")
    List<Object[]> countPublishedGroupByAuthorId();

    @Query("select p.category.id, count(p) from Post p where p.category is not null and p.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED group by p.category.id")
    List<Object[]> countPublishedGroupByCategoryId();
}
