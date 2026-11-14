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

public interface AdminPostRepository extends JpaRepository<Post, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findLockedById(@Param("id") String id);
    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where (:status is null or p.status = :status) " +
            "and lower(p.title) like lower(concat('%', :search, '%'))")
    Page<Post> search(@Param("status") Post.PostStatus status, @Param("search") String search, Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    @Query("select p.createdAt from Post p where p.createdAt >= :from and p.createdAt < :to")
    List<LocalDateTime> findCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByAuthorId(String authorId);

    @Query("select p.author.id, count(p) from Post p where p.author.id in :authorIds group by p.author.id")
    List<Object[]> countByAuthorIds(@Param("authorIds") Collection<String> authorIds);

    @Query("select coalesce(sum(p.viewsCount), 0) from Post p")
    long countTotalViews();

    /** [name, count] — số bài đã đăng theo danh mục, giảm dần */
    @Query("select c.name, count(p) from Post p join p.category c " +
            "where p.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED " +
            "group by c.id, c.name order by count(p) desc")
    List<Object[]> countByCategory(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<Post> findTop5ByOrderByCreatedAtDesc();
}
