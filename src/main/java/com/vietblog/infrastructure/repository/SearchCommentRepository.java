package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCommentRepository extends JpaRepository<Comment, String> {}
