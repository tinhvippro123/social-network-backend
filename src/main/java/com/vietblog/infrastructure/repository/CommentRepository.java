package com.vietblog.infrastructure.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.vietblog.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    @Modifying
    @Query("update Comment c set c.parent = null where c.post.id = :postId")
    void detachForPost(@Param("postId") String postId);
    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteForPost(@Param("postId") String postId);
    long countByPostId(String postId);
    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(String postId);

    @Query("select c from Comment c left join fetch c.author left join fetch c.parent p left join fetch p.author where c.post.id = :postId order by c.createdAt asc, c.id asc")
    List<Comment> findAllByPostIdWithAuthor(@Param("postId") String postId);

    @Query("select c.id, c.post.id from Comment c where c.id in :ids")
    List<Object[]> findPostIdsByCommentIds(@Param("ids") java.util.Collection<String> ids);
}
