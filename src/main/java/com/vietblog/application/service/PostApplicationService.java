package com.vietblog.application.service;

import java.util.Comparator;
import java.util.ArrayList;
import org.springframework.data.domain.Pageable;
import com.vietblog.infrastructure.repository.PostImageFeaturesRepository;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.ReactionRepository;
import com.vietblog.infrastructure.repository.CommentRepository;
import com.vietblog.application.dto.post.CreatePostRequest;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ForbiddenException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service: Điều phối (Orchestration).
 * Lấy data từ DB -> Đưa cho Domain Service xử lý (nếu có) -> Lưu lại DB.
 */
@Service
@RequiredArgsConstructor
public class PostApplicationService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostDomainService postDomainService;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final BookmarkRepository bookmarks;
    private final PostImageFeaturesRepository imageFeatures;

    @Transactional
    public PostResponse createPost(String authorId, CreatePostRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        
        Category category = categoryRepository.findBySlug(request.getCategoryId())
                .or(() -> categoryRepository.findById(request.getCategoryId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        Post.PostStatus status = Post.PostStatus.PUBLISHED;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                status = Post.PostStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
            }
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .coverImage(request.getCoverImage())
                .author(author)
                .category(category)
                .tags(request.getTags() == null ? new ArrayList<>() : request.getTags())
                .status(status)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        // Uỷ quyền cho Domain Service kiểm tra nghiệp vụ
        postDomainService.validatePostContent(post);

        // Lưu vào cơ sở hạ tầng
        Post savedPost = postRepository.save(post);
        
        return PostResponse.fromEntity(savedPost);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getLatestPosts(int page, int limit) {
        validatePage(page, limit);
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByStatus(Post.PostStatus.PUBLISHED, pageRequest)
                .map(PostResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getTrendingPosts(int limit) {
        validatePage(1, limit);
        // Recalculate decay at read time so quiet posts cannot retain stale rankings.
        return postRepository.findByStatus(Post.PostStatus.PUBLISHED, Pageable.unpaged())
                .stream().peek(postDomainService::calculateTrendingScore)
                .sorted(Comparator.comparingDouble(Post::getTrendingScore).reversed().thenComparing(Post::getId))
                .limit(limit)
                .map(PostResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public PostResponse getPostById(String id) {
        return getPostById(id, null);
    }

    @Transactional
    public PostResponse getPostById(String id, String viewerId) {
        Post post = postRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        if (post.getStatus() != Post.PostStatus.PUBLISHED && !post.getAuthor().getId().equals(viewerId)) {
            throw new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND);
        }
        
        // Tăng view count
        post.setViewsCount(post.getViewsCount() + 1);
        
        // Nhờ Domain tính lại điểm trending vì view tăng
        postDomainService.calculateTrendingScore(post);
        
        postRepository.save(post);
        
        return PostResponse.fromEntity(post);
    }

    @Transactional
    public PostResponse updatePost(String id, String userId, CreatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        // Kiểm tra quyền sở hữu: Chỉ tác giả mới được cập nhật bài viết
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        if (post.getStatus() == Post.PostStatus.HIDDEN) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        Category category = categoryRepository.findBySlug(request.getCategoryId())
                .or(() -> categoryRepository.findById(request.getCategoryId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        Post.PostStatus status = post.getStatus();
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                status = Post.PostStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
            }
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setExcerpt(request.getExcerpt());
        post.setCoverImage(request.getCoverImage());
        post.setCategory(category);
        post.setTags(request.getTags() == null ? new ArrayList<>() : request.getTags());
        post.setStatus(status);
        post.setLatitude(request.getLatitude());
        post.setLongitude(request.getLongitude());

        postDomainService.validatePostContent(post);

        Post updatedPost = postRepository.save(post);
        return PostResponse.fromEntity(updatedPost);
    }

    @Transactional
    public void deletePost(String id, String userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        // Kiểm tra quyền sở hữu: Chỉ tác giả mới được xóa bài viết
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        imageFeatures.findById(id).ifPresent(imageFeatures::delete);
        comments.detachForPost(id);
        comments.deleteForPost(id);
        reactions.deleteForPost(id);
        bookmarks.deleteForPost(id);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> feed(int page, int limit, String category, String query) {
        validatePage(page, limit);
        if (query != null && query.length() > 1000) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        return postRepository.findFeed(category, query,
                PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(PostResponse::fromEntity);
    }

    private static void validatePage(int page, int limit) {
        if (page < 1 || limit < 1 || limit > 100) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
    }
}
