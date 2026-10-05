package com.vietblog.infrastructure.repository;

import com.vietblog.domain.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, String> {
    Optional<Reaction> findByUserIdAndPostId(String userId, String postId);
    List<Reaction> findByPostId(String postId);
    void deleteByUserIdAndPostId(String userId, String postId);

    @Query("SELECT r.emoji, COUNT(r) FROM Reaction r WHERE r.post.id = :postId GROUP BY r.emoji")
    List<Object[]> countByPostIdGroupByEmoji(@Param("postId") String postId);
}
