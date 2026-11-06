package com.vietblog.application.service;

import com.vietblog.application.dto.report.ReportDtos;
import com.vietblog.domain.entity.Comment;
import com.vietblog.domain.entity.Group;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.Report;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ForbiddenException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.AdminPostRepository;
import com.vietblog.infrastructure.repository.AdminUserRepository;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.CommentRepository;
import com.vietblog.infrastructure.repository.GroupMemberRepository;
import com.vietblog.infrastructure.repository.GroupRepository;
import com.vietblog.infrastructure.repository.NotificationRepository;
import com.vietblog.infrastructure.repository.ReportRepository;
import com.vietblog.domain.service.PostDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reports;
    private final AdminUserRepository users;
    private final AdminPostRepository posts;
    private final CommentRepository comments;
    private final NotificationRepository notifications;
    private final GroupRepository groups;
    private final GroupMemberRepository groupMembers;
    private final GroupService groupService;
    private final CategoryRepository categories;
    private final PostDomainService postDomainService;

    @Transactional
    public ReportDtos.Response create(String userId, ReportDtos.CreateRequest request) {
        User reporter = users.findLockedById(userId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        String title = targetTitle(request.type(), request.targetId(), userId, request.reason());
        if (reports.existsByReporterIdAndTargetIdAndTypeAndStatus(userId, request.targetId(), request.type(), Report.ReportStatus.PENDING)) {
            throw new BusinessRuleException(ErrorCode.REPORT_ALREADY_PENDING);
        }
        Report report = Report.builder().reporter(reporter).targetId(request.targetId()).type(request.type())
                .targetTitle(title).reason(request.reason().trim()).description(request.description()).build();
        return toResponse(reports.save(report));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional(readOnly = true)
    public Page<ReportDtos.Response> list(Report.ReportStatus status, int page, int limit) {
        var pageable = AdminService.page(page, limit, "createdAt");
        return (status == null ? reports.findAll(pageable) : reports.findByStatus(status, pageable)).map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional
    public ReportDtos.Response resolve(String id, String actorId, ReportDtos.Action action) {
        Report report = pending(id);
        User actor = users.findById(actorId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        switch (action) {
            case KEEP -> { }
            case DELETE -> removeTarget(report, actor);
            case BAN -> banTarget(report, actor);
        }
        ReportDtos.Response res = finish(report, actor, Report.ReportStatus.RESOLVED, action.name());
        if (action == ReportDtos.Action.DELETE || action == ReportDtos.Action.BAN) {
            for (Report dup : reports.findByTypeAndTargetIdAndStatus(report.getType(), report.getTargetId(), Report.ReportStatus.PENDING)) {
                if (!dup.getId().equals(report.getId())) {
                    finish(dup, actor, Report.ReportStatus.RESOLVED, action.name());
                }
            }
        }
        return res;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional
    public ReportDtos.Response dismiss(String id, String actorId) {
        Report report = pending(id);
        User actor = users.findById(actorId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return finish(report, actor, Report.ReportStatus.DISMISSED, "DISMISS");
    }

    private Report pending(String id) {
        Report report = reports.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REPORT_NOT_FOUND));
        if (report.getStatus() != Report.ReportStatus.PENDING) throw new BusinessRuleException(ErrorCode.REPORT_ALREADY_RESOLVED);
        return report;
    }

    private ReportDtos.Response finish(Report report, User actor, Report.ReportStatus status, String action) {
        report.setStatus(status);
        report.setResolutionAction(action);
        report.setResolvedBy(actor);
        report.setResolvedAt(LocalDateTime.now());
        reports.save(report);
        String statusLabel = status == Report.ReportStatus.RESOLVED ? "Đã xử lý" : "Đã từ chối";
        notifications.save(Notification.builder().user(report.getReporter()).type(Notification.NotificationType.REPORT)
                .message("Báo cáo của bạn đã được bộ phận kiểm duyệt xem xét: " + statusLabel).link("/notifications").build());
        return toResponse(report);
    }

    private ReportDtos.Response toResponse(Report report) {
        String targetPostId = null;
        if (report.getType() == Report.TargetType.POST) {
            targetPostId = report.getTargetId();
        } else if (report.getType() == Report.TargetType.COMMENT) {
            targetPostId = comments.findById(report.getTargetId()).map(c -> c.getPost().getId()).orElse(null);
        }
        return ReportDtos.Response.from(report, targetPostId);
    }

    private String targetTitle(Report.TargetType type, String id, String userId, String reason) {
        return switch (type) {
            case POST -> {
                Post post = publishedPost(id);
                if (post.getAuthor() != null && userId.equals(post.getAuthor().getId())) {
                    throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Bạn không thể tự báo cáo bài viết của chính mình");
                }
                yield post.getTitle();
            }
            case COMMENT -> {
                Comment comment = comment(id);
                if (comment.getPost().getStatus() != Post.PostStatus.PUBLISHED) throw new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
                if (comment.getAuthor() != null && userId.equals(comment.getAuthor().getId())) {
                    throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Bạn không thể tự báo cáo bình luận của chính mình");
                }
                yield comment.getContent().substring(0, Math.min(255, comment.getContent().length()));
            }
            case USER -> {
                User targetUser = users.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
                boolean isAccountRequest = reason != null && reason.trim().startsWith("Yêu cầu ");
                if (userId != null && userId.equals(id) && !isAccountRequest) {
                    throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Bạn không thể tự báo cáo tài khoản của chính mình");
                }
                yield targetUser.getName();
            }
            case GROUP -> {
                Group group = group(id);
                if (!group.isPublicGroup() && !groupMembers.existsByGroupIdAndUserId(id, userId)) {
                    throw new ForbiddenException(ErrorCode.FORBIDDEN);
                }
                if (group.getOwner() != null && userId.equals(group.getOwner().getId())) {
                    throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Bạn không thể tự báo cáo nhóm của chính mình");
                }
                yield group.getName();
            }
        };
    }

    private Post publishedPost(String id) {
        Post post = post(id);
        if (post.getStatus() != Post.PostStatus.PUBLISHED) throw new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND);
        return post;
    }

    private Post post(String id) {
        return posts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
    }

    private Comment comment(String id) {
        return comments.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private Group group(String id) {
        return groups.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GROUP_NOT_FOUND));
    }

    private void removeTarget(Report report, User actor) {
        switch (report.getType()) {
            case POST -> posts.findById(report.getTargetId()).ifPresent(post -> {
                boolean wasPublished = post.getStatus() == Post.PostStatus.PUBLISHED;
                post.setStatus(Post.PostStatus.HIDDEN);
                posts.save(post);
                if (wasPublished) {
                    if (post.getAuthor() != null) {
                        post.getAuthor().setPostsCount(Math.max(0, post.getAuthor().getPostsCount() - 1));
                        users.save(post.getAuthor());
                    }
                    if (post.getCategory() != null) {
                        post.getCategory().setPostsCount(Math.max(0, post.getCategory().getPostsCount() - 1));
                        if (categories != null) {
                            categories.save(post.getCategory());
                        }
                    }
                }
            });
            case COMMENT -> comments.findById(report.getTargetId()).ifPresent(comment -> {
                Post post = comment.getPost();
                comments.delete(comment);
                comments.flush();
                if (post != null) {
                    post.setCommentsCount(Math.toIntExact(comments.countByPostId(post.getId())));
                    if (postDomainService != null) {
                        postDomainService.calculateTrendingScore(post);
                    }
                    posts.save(post);
                }
            });
            case GROUP -> {
                if (actor.getRole() != User.Role.ADMIN) throw new ForbiddenException(ErrorCode.FORBIDDEN);
                if (groups.existsById(report.getTargetId())) {
                    groupService.delete(report.getTargetId(), actor.getId());
                }
            }
            case USER -> banTarget(report, actor);
        }
    }

    private void banTarget(Report report, User actor) {
        String targetUserId = switch (report.getType()) {
            case POST -> posts.findById(report.getTargetId()).map(p -> p.getAuthor().getId()).orElse(null);
            case COMMENT -> comments.findById(report.getTargetId()).map(c -> c.getAuthor().getId()).orElse(null);
            case USER -> report.getTargetId();
            case GROUP -> groups.findById(report.getTargetId()).map(g -> g.getOwner().getId()).orElse(null);
        };
        if (targetUserId == null) return;
        User target = users.findLockedById(targetUserId).orElse(null);
        if (target == null) return;
        if (target.getId().equals(actor.getId()) || target.getRole() == User.Role.ADMIN
                || (actor.getRole() == User.Role.MODERATOR && target.getRole() != User.Role.USER)) {
            throw new ForbiddenException(ErrorCode.PROTECTED_ADMIN_ACCOUNT);
        }
        target.setActive(false);
        users.save(target);
    }
}
