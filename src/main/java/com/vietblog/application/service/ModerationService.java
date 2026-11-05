package com.vietblog.application.service;

import com.vietblog.application.dto.admin.AdminDtos;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ModerationService {
    private final PostRepository posts;
    private final UserRepository users;
    private final CategoryRepository categories;

    public ModerationService(PostRepository posts) {
        this(posts, null, null);
    }

    @Autowired
    public ModerationService(PostRepository posts, UserRepository users, CategoryRepository categories) {
        this.posts = posts;
        this.users = users;
        this.categories = categories;
    }

    @Transactional
    public AdminDtos.PostResponse status(String id, Post.PostStatus status) {
        if (status == Post.PostStatus.DRAFT) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        var post = posts.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        // Drafts belong to the author until they choose to publish.
        if (post.getStatus() == Post.PostStatus.DRAFT) throw new ForbiddenException(ErrorCode.FORBIDDEN);
        post.setStatus(status);
        posts.saveAndFlush(post);
        if (post.getAuthor() != null && users != null) {
            int authorCount = Math.toIntExact(posts.countByAuthorIdAndStatus(post.getAuthor().getId(), Post.PostStatus.PUBLISHED));
            post.getAuthor().setPostsCount(authorCount);
            users.save(post.getAuthor());
        }
        if (post.getCategory() != null && categories != null) {
            int catCount = Math.toIntExact(posts.countByCategoryIdAndStatus(post.getCategory().getId(), Post.PostStatus.PUBLISHED));
            post.getCategory().setPostsCount(catCount);
            categories.save(post.getCategory());
        }
        return AdminDtos.PostResponse.from(post);
    }
}
