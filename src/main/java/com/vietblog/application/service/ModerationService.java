package com.vietblog.application.service;

import com.vietblog.application.dto.admin.AdminDtos;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ModerationService {
    private final PostRepository posts;
    @Transactional
    public AdminDtos.PostResponse status(String id, Post.PostStatus status) {
        if (status == Post.PostStatus.DRAFT) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        var post = posts.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        // Drafts belong to the author until they choose to publish.
        if (post.getStatus() == Post.PostStatus.DRAFT) throw new ForbiddenException(ErrorCode.FORBIDDEN);
        post.setStatus(status);
        return AdminDtos.PostResponse.from(post);
    }
}
