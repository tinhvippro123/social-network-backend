package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminPostRepository extends JpaRepository<Post, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    java.util.Optional<Post> findLockedById(@Param("id") String id);
    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where (:status is null or p.status = :status) " +
            "and lower(p.title) like lower(concat('%', :search, '%'))")
    Page<Post> search(@Param("status") Post.PostStatus status, @Param("search") String search, Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(p.viewsCount), 0) from Post p")
    long countTotalViews();
}
