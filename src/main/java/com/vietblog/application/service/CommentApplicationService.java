package com.vietblog.application.service;

import com.vietblog.application.port.CacheGateway;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.application.dto.comment.CommentResponse;
import com.vietblog.application.dto.comment.CreateCommentRequest;
import com.vietblog.domain.entity.Comment;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ForbiddenException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.service.CommentDomainService;
import com.vietblog.infrastructure.repository.CommentRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentApplicationService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentDomainService commentDomainService;
    private final NotificationService notificationService;
    private final PostDomainService postDomainService;
    private final CacheGateway cache;

    @Transactional
    public CommentResponse addComment(String postId, String authorId, CreateCommentRequest request) {
        commentDomainService.validateCommentContent(request.getContent());

        Post post = postRepository.findLockedById(postId)
                .filter(item -> item.getStatus() == Post.PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
            
            // Đảm bảo comment cha thuộc cùng một bài viết
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new BusinessRuleException(ErrorCode.PARENT_COMMENT_NOT_IN_POST);
            }
            
            commentDomainService.validateNestingLevel(parentComment);
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .author(author)
                .post(post)
                .parent(parentComment)
                .build();

        comment = commentRepository.save(comment);
        post.setCommentsCount(post.getCommentsCount() + 1);
        postDomainService.calculateTrendingScore(post);
        cache.evict(PostApplicationService.TRENDING_IDS_CACHE_KEY);
        if (parentComment != null && !parentComment.getAuthor().getId().equals(authorId)) {
            notificationService.notifyWithPreferences(
                    parentComment.getAuthor(),
                    Notification.NotificationType.COMMENT,
                    author.getName() + " đã trả lời bình luận của bạn",
                    author.getAvatar(),
                    "/posts/" + postId + "#comment-" + comment.getId(),
                    "[VietBlog] " + author.getName() + " đã trả lời bình luận của bạn",
                    "Phản hồi bình luận mới"
            );
        }
        if (!post.getAuthor().getId().equals(authorId)
                && (parentComment == null || !parentComment.getAuthor().getId().equals(post.getAuthor().getId()))) {
            notificationService.notifyWithPreferences(
                    post.getAuthor(),
                    Notification.NotificationType.COMMENT,
                    author.getName() + " đã bình luận về bài viết của bạn",
                    author.getAvatar(),
                    "/posts/" + postId + "#comment-" + comment.getId(),
                    "[VietBlog] " + author.getName() + " đã bình luận bài viết \"" + post.getTitle() + "\"",
                    "Bình luận mới trên bài viết của bạn"
            );
        }

        // Lấy lại từ DB để lấy được toàn bộ context nếu cần (đặc biệt khi lưu xong)
        return CommentResponse.fromEntity(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(String postId) {
        postRepository.findById(postId).filter(post -> post.getStatus() == Post.PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        // Chỉ lấy các comment bậc 1 (parent == null), 
        // bên trong CommentResponse.fromEntity sẽ đệ quy tự động map các replies
        List<Comment> allComments = commentRepository.findAllByPostIdWithAuthor(postId);
        java.util.Map<String, List<Comment>> repliesByParentId = new java.util.HashMap<>();
        List<Comment> topLevelComments = new ArrayList<>();
        for (Comment comment : allComments) {
            if (comment.getParent() == null) {
                topLevelComments.add(comment);
            } else {
                repliesByParentId
                        .computeIfAbsent(comment.getParent().getId(), ignored -> new ArrayList<>())
                        .add(comment);
            }
        }
        topLevelComments.sort(java.util.Comparator
                .comparing(Comment::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .reversed());

        return topLevelComments.stream()
                .map(comment -> CommentResponse.fromEntity(comment, repliesByParentId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        // Kiểm tra quyền: Tác giả comment, tác giả bài viết hoặc Admin/Moderator mới được xóa
        if (!comment.getAuthor().getId().equals(userId)) {
            boolean isPostOwner = comment.getPost() != null
                    && comment.getPost().getAuthor() != null
                    && comment.getPost().getAuthor().getId().equals(userId);
            boolean isAdminOrMod = userRepository.findById(userId)
                    .map(u -> u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.MODERATOR)
                    .orElse(false);
            if (!isPostOwner && !isAdminOrMod) {
                throw new ForbiddenException(ErrorCode.COMMENT_DELETE_FORBIDDEN);
            }
        }

        Post post = postRepository.findLockedById(comment.getPost().getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        promoteChildRepliesBeforeDelete(commentRepository, comment);
        commentRepository.delete(comment);
        commentRepository.flush();
        post.setCommentsCount(Math.toIntExact(commentRepository.countByPostId(post.getId())));
        postDomainService.calculateTrendingScore(post);
        cache.evict(PostApplicationService.TRENDING_IDS_CACHE_KEY);
    }

    /**
     * Khi xóa bình luận trả lời (cấp con), chuyển các bình luận trả lời của nó lên bình luận cha gốc
     * và lưu lại thông tin @replyTo để giữ nguyên ngữ cảnh cuộc trò chuyện (chuẩn Facebook).
     */
    public static void promoteChildRepliesBeforeDelete(CommentRepository commentRepository, Comment comment) {
        if (comment == null || comment.getParent() == null
                || comment.getReplies() == null || comment.getReplies().isEmpty()) {
            return;
        }
        Comment upperParent = comment.getParent();
        String targetName = comment.getAuthor() != null ? comment.getAuthor().getName() : null;
        String targetAuthorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;

        List<Comment> children = new ArrayList<>(comment.getReplies());
        comment.getReplies().clear();
        if (upperParent.getReplies() != null) {
            upperParent.getReplies().remove(comment);
        }

        for (Comment child : children) {
            child.setParent(upperParent);
            if (upperParent.getReplies() != null && !upperParent.getReplies().contains(child)) {
                upperParent.getReplies().add(child);
            }
            if (targetName != null
                    && (child.getAuthor() == null || !child.getAuthor().getId().equals(targetAuthorId))
                    && !CommentResponse.hasReplyToPrefix(child.getContent())) {
                String currentContent = child.getContent() != null ? child.getContent() : "";
                child.setContent(CommentResponse.formatReplyToPrefix(targetName, targetAuthorId) + currentContent);
            }
            commentRepository.save(child);
        }
        commentRepository.flush();
    }

    @Transactional
    public void deleteComment(String postId, String commentId, String userId) {
        commentRepository.findById(commentId).filter(comment -> comment.getPost().getId().equals(postId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        deleteComment(commentId, userId);
    }
}
