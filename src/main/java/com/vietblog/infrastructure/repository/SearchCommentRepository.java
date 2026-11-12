package com.vietblog.infrastructure.repository;

import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import com.vietblog.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCommentRepository extends JpaRepository<Comment, String> {
    @Query("select c.content from Comment c where c.post.status = com.vietblog.domain.entity.Post.PostStatus.PUBLISHED")
    Stream<String> streamContents();
}
