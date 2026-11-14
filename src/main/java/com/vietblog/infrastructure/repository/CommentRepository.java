package com.vietblog.infrastructure.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.vietblog.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
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
    List<Object[]> findPostIdsByCommentIds(@Param("ids") Collection<String> ids);

    @Query("select c.post.id, count(c) from Comment c where c.post.id in :postIds group by c.post.id")
    List<Object[]> countByPostIds(@Param("postIds") Collection<String> postIds);

    @Query("select c.post.id, count(c) from Comment c group by c.post.id")
    List<Object[]> countAllGroupByPostId();
}
