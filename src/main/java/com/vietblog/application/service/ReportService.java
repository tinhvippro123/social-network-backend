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
import com.vietblog.infrastructure.repository.CommentRepository;
import com.vietblog.infrastructure.repository.ReportRepository;
import com.vietblog.infrastructure.repository.GroupRepository;
import com.vietblog.infrastructure.repository.GroupMemberRepository;
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
    private final com.vietblog.infrastructure.repository.NotificationRepository notifications;
    private final GroupRepository groups;
    private final GroupMemberRepository groupMembers;
    private final GroupService groupService;

    @Transactional
    public ReportDtos.Response create(String userId, ReportDtos.CreateRequest request) {
        // Serialize submissions by one reporter so simultaneous duplicate requests cannot create two pending reports.
        User reporter = users.findLockedById(userId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        String title = targetTitle(request.type(), request.targetId(), userId);
        if (reports.existsByReporterIdAndTargetIdAndTypeAndStatus(userId, request.targetId(), request.type(), Report.ReportStatus.PENDING)) {
            throw new BusinessRuleException(ErrorCode.REPORT_ALREADY_PENDING);
        }
        Report report = Report.builder().reporter(reporter).targetId(request.targetId()).type(request.type())
                .targetTitle(title).reason(request.reason().trim()).description(request.description()).build();
        return ReportDtos.Response.from(reports.save(report));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional(readOnly = true)
    public Page<ReportDtos.Response> list(Report.ReportStatus status, int page, int limit) {
        var pageable = AdminService.page(page, limit, "createdAt");
        return (status == null ? reports.findAll(pageable) : reports.findByStatus(status, pageable)).map(ReportDtos.Response::from);
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
        return finish(report, actor, Report.ReportStatus.RESOLVED, action.name());
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
        notifications.save(Notification.builder().user(report.getReporter()).type(Notification.NotificationType.REPORT)
                .message("Your report has been reviewed: " + status.name().toLowerCase(java.util.Locale.ROOT)).link("/notifications").build());
        return ReportDtos.Response.from(report);
    }

    private String targetTitle(Report.TargetType type, String id, String userId) {
        return switch (type) {
            case POST -> publishedPost(id).getTitle();
            case COMMENT -> {
                Comment comment = comment(id);
                if (comment.getPost().getStatus() != Post.PostStatus.PUBLISHED) throw new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
                yield comment.getContent().substring(0, Math.min(255, comment.getContent().length()));
            }
            case USER -> users.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)).getName();
            case GROUP -> {
                Group group = group(id);
                if (!group.isPublicGroup() && !groupMembers.existsByGroupIdAndUserId(id, userId)) {
                    throw new ForbiddenException(ErrorCode.FORBIDDEN);
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
            case POST -> {
                Post post = post(report.getTargetId());
                post.setStatus(Post.PostStatus.HIDDEN);
                posts.save(post);
            }
            case COMMENT -> {
                Comment comment = comment(report.getTargetId());
                Post post = comment.getPost();
                comments.delete(comment);
                comments.flush();
                post.setCommentsCount(Math.toIntExact(comments.countByPostId(post.getId())));
                posts.save(post);
            }
            case GROUP -> {
                if (actor.getRole() != User.Role.ADMIN) throw new ForbiddenException(ErrorCode.FORBIDDEN);
                groupService.delete(report.getTargetId(), actor.getId());
            }
            case USER -> throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
    }

    private void banTarget(Report report, User actor) {
        String targetUserId = switch (report.getType()) {
            case POST -> post(report.getTargetId()).getAuthor().getId();
            case COMMENT -> comment(report.getTargetId()).getAuthor().getId();
            case USER -> report.getTargetId();
            case GROUP -> group(report.getTargetId()).getOwner().getId();
        };
        User target = users.findLockedById(targetUserId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (target.getId().equals(actor.getId()) || target.getRole() == User.Role.ADMIN
                || (actor.getRole() == User.Role.MODERATOR && target.getRole() != User.Role.USER)) {
            throw new ForbiddenException(ErrorCode.PROTECTED_ADMIN_ACCOUNT);
        }
        target.setActive(false);
        users.save(target);
    }
}
