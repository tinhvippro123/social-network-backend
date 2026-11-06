package com.vietblog.application.service;

import com.vietblog.application.dto.post.CreatePostRequest;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ForbiddenException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.search.TextAlgorithms;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.CommentRepository;
import com.vietblog.infrastructure.repository.PostImageFeaturesRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.ReactionRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final CoverImageIndexer coverImageIndexer;
    private final HtmlSanitizer htmlSanitizer;

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

        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());
        String resolvedExcerpt = resolveExcerpt(request.getExcerpt(), sanitizedContent);

        Post post = Post.builder()
                .title(request.getTitle().trim())
                .content(sanitizedContent)
                .excerpt(resolvedExcerpt)
                .coverImage(request.getCoverImage())
                .author(author)
                .category(category)
                .tags(sanitizeTags(request.getTags()))
                .status(status)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationAddress(request.getLatitude() == null ? null : request.getLocationAddress())
                .eventStartTime(request.getEventStartTime())
                .eventEndTime(request.getEventEndTime())
                .build();

        postDomainService.validatePostContent(post);
        postDomainService.calculateTrendingScore(post);

        Post savedPost = postRepository.save(post);
        syncAuthorAndCategoryCounters(author, category);
        coverImageIndexer.scheduleAfterCommit(savedPost.getId());

        return mapPostResponse(savedPost, authorId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getLatestPosts(int page, int limit) {
        validatePage(page, limit);
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByStatus(Post.PostStatus.PUBLISHED, pageRequest)
                .map(post -> mapPostResponse(post, null));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getTrendingPosts(int limit) {
        return getTrendingPosts(limit, null);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getTrendingPosts(int limit, String viewerId) {
        validatePage(1, limit);
        return postRepository.findByStatus(Post.PostStatus.PUBLISHED, Pageable.unpaged())
                .stream().peek(postDomainService::calculateTrendingScore)
                .sorted(Comparator.comparingDouble(Post::getTrendingScore).reversed().thenComparing(Post::getId))
                .limit(limit)
                .map(post -> mapPostResponse(post, viewerId))
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
        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            boolean isAuthor = viewerId != null && post.getAuthor() != null && post.getAuthor().getId().equals(viewerId);
            boolean isAdminOrMod = viewerId != null && userRepository.findById(viewerId)
                    .map(u -> u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.MODERATOR)
                    .orElse(false);
            if (!isAuthor && !isAdminOrMod) {
                throw new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND);
            }
        } else {
            post.setViewsCount(post.getViewsCount() + 1);
            postDomainService.calculateTrendingScore(post);
            postRepository.save(post);
        }

        return mapPostResponse(post, viewerId);
    }

    @Transactional
    public PostResponse updatePost(String id, String userId, CreatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        if (post.getStatus() == Post.PostStatus.HIDDEN) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        Category oldCategory = post.getCategory();
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

        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());
        String resolvedExcerpt = resolveExcerpt(request.getExcerpt(), sanitizedContent);

        post.setTitle(request.getTitle().trim());
        post.setContent(sanitizedContent);
        post.setExcerpt(resolvedExcerpt);
        post.setCoverImage(request.getCoverImage());
        post.setCategory(category);
        post.setTags(sanitizeTags(request.getTags()));
        post.setStatus(status);
        post.setLatitude(request.getLatitude());
        post.setLongitude(request.getLongitude());
        post.setLocationAddress(request.getLatitude() == null ? null : request.getLocationAddress());
        post.setEventStartTime(request.getEventStartTime());
        post.setEventEndTime(request.getEventEndTime());

        postDomainService.validatePostContent(post);
        postDomainService.calculateTrendingScore(post);

        Post updatedPost = postRepository.save(post);
        syncAuthorAndCategoryCounters(post.getAuthor(), category);
        if (oldCategory != null && !oldCategory.getId().equals(category.getId())) {
            syncCategoryCounter(oldCategory);
        }
        coverImageIndexer.scheduleAfterCommit(updatedPost.getId());
        return mapPostResponse(updatedPost, userId);
    }

    @Transactional
    public void deletePost(String id, String userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAuthor().getId().equals(userId)) {
            boolean isAdmin = userRepository.findById(userId)
                    .map(u -> u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.MODERATOR)
                    .orElse(false);
            if (!isAdmin) {
                throw new ForbiddenException(ErrorCode.FORBIDDEN);
            }
        }

        User author = post.getAuthor();
        Category category = post.getCategory();

        imageFeatures.findById(id).ifPresent(imageFeatures::delete);
        comments.detachForPost(id);
        comments.deleteForPost(id);
        reactions.deleteForPost(id);
        bookmarks.deleteForPost(id);
        postRepository.delete(post);
        postRepository.flush();

        syncAuthorAndCategoryCounters(author, category);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> feed(int page, int limit, String category, String query) {
        return feed(page, limit, category, query, null, "latest");
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> feed(int page, int limit, String category, String query, String viewerId) {
        return feed(page, limit, category, query, viewerId, "latest");
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> feed(int page, int limit, String category, String query, String viewerId, String sort) {
        validatePage(page, limit);
        if (query != null && query.length() > 1000) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);

        boolean hasCategory = category != null && !category.isBlank() && !"all".equalsIgnoreCase(category.trim());
        boolean hasQuery = query != null && !query.trim().isBlank();
        Sort sortSpec = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        if ("trending".equalsIgnoreCase(sort)) {
            sortSpec = Sort.by(Sort.Direction.DESC, "trendingScore", "viewsCount", "id");
        } else if ("popular".equalsIgnoreCase(sort)) {
            sortSpec = Sort.by(Sort.Direction.DESC, "viewsCount", "upvotesCount", "id");
        }
        PageRequest pageRequest = PageRequest.of(page - 1, limit, sortSpec);

        Page<Post> postPage;
        if (hasCategory && hasQuery) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            postPage = postRepository.findByCategoryAndKeyword(category.trim(), pattern, pageRequest);
        } else if (hasCategory) {
            postPage = postRepository.findByCategory(category.trim(), pageRequest);
        } else if (hasQuery) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            postPage = postRepository.findByKeyword(pattern, pageRequest);
        } else {
            postPage = postRepository.findByStatus(Post.PostStatus.PUBLISHED, pageRequest);
        }

        return postPage.map(post -> mapPostResponse(post, viewerId));
    }

    public void syncAuthorAndCategoryCounters(User author, Category category) {
        if (author != null) {
            int authorCount = Math.toIntExact(postRepository.countByAuthorIdAndStatus(author.getId(), Post.PostStatus.PUBLISHED));
            author.setPostsCount(authorCount);
            userRepository.save(author);
        }
        syncCategoryCounter(category);
    }

    private void syncCategoryCounter(Category category) {
        if (category != null) {
            int catCount = Math.toIntExact(postRepository.countByCategoryIdAndStatus(category.getId(), Post.PostStatus.PUBLISHED));
            category.setPostsCount(catCount);
            categoryRepository.save(category);
        }
    }

    private static List<String> sanitizeTags(List<String> tags) {
        if (tags == null) return new ArrayList<>();
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .limit(10)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String resolveExcerpt(String excerpt, String sanitizedContent) {
        if (excerpt != null && !excerpt.isBlank()) {
            return excerpt.trim();
        }
        String plain = TextAlgorithms.plainText(sanitizedContent == null ? "" : sanitizedContent).trim();
        if (plain.isEmpty()) return "";
        return plain.length() <= 200 ? plain : plain.substring(0, 197) + "...";
    }

    private PostResponse mapPostResponse(Post post, String viewerId) {
        int authorPostsCount = post.getAuthor() != null
                ? Math.max(post.getAuthor().getPostsCount(),
                        (int) postRepository.countByAuthorIdAndStatus(post.getAuthor().getId(), Post.PostStatus.PUBLISHED))
                : 0;
        boolean isBookmarked = viewerId != null && bookmarks.existsByUserIdAndPostId(viewerId, post.getId());
        return PostResponse.fromEntity(post, authorPostsCount, isBookmarked);
    }

    private static void validatePage(int page, int limit) {
        if (page < 1 || limit < 1 || limit > 100) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
    }
}
