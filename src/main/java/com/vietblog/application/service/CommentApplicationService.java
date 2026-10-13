package com.vietblog.application.service;

import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.application.dto.comment.CommentResponse;
import com.vietblog.application.dto.comment.CreateCommentRequest;
import com.vietblog.domain.entity.Comment;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentApplicationService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentDomainService commentDomainService;
    private final NotificationService notifications;
    private final PostDomainService postDomainService;

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
        if (!post.getAuthor().getId().equals(authorId)) {
            notifications.notify(post.getAuthor(), Notification.NotificationType.COMMENT,
                    author.getName() + " commented on your post", author.getAvatar(), "/posts/" + postId);
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
        List<Comment> topLevelComments = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId);
        
        return topLevelComments.stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        // Kiểm tra quyền: Chỉ tác giả comment mới được xóa
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        Post post = postRepository.findLockedById(comment.getPost().getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        commentRepository.delete(comment);
        commentRepository.flush();
        post.setCommentsCount(Math.toIntExact(commentRepository.countByPostId(post.getId())));
        postDomainService.calculateTrendingScore(post);
    }

    @Transactional
    public void deleteComment(String postId, String commentId, String userId) {
        commentRepository.findById(commentId).filter(comment -> comment.getPost().getId().equals(postId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        deleteComment(commentId, userId);
    }
}
