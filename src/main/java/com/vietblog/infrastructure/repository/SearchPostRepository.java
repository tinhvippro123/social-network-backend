package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SearchPostRepository extends JpaRepository<Post, String> {
    @EntityGraph(attributePaths = {"author", "category"})
    List<Post> findByStatusOrderByCreatedAtDescIdAsc(Post.PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("select p from Post p where p.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED and p.id in :ids")
    List<Post> findPublishedByIdIn(@Param("ids") Collection<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findLockedById(@Param("id") String id);
}
