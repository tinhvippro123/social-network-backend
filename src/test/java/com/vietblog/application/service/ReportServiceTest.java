package com.vietblog.application.service;

import com.vietblog.application.dto.report.ReportDtos;
import com.vietblog.domain.entity.*;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock ReportRepository reports;
    @Mock AdminUserRepository users;
    @Mock AdminPostRepository posts;
    @Mock CommentRepository comments;
    @Mock NotificationService notifications;
    @Mock GroupRepository groups;
    @Mock GroupMemberRepository groupMembers;
    @Mock GroupService groupService;
    @InjectMocks ReportService service;

    private final User reporter = User.builder().id("reporter").name("Reporter").build();
    private final User admin = User.builder().id("admin").role(User.Role.ADMIN).build();

    @Test
    void cannotReportHiddenPost() {
        when(users.findLockedById("reporter")).thenReturn(Optional.of(reporter));
        when(posts.findById("post")).thenReturn(Optional.of(Post.builder().id("post").status(Post.PostStatus.HIDDEN).build()));
        assertThrows(ResourceNotFoundException.class,
                () -> service.create("reporter", new ReportDtos.CreateRequest("post", Report.TargetType.POST, "spam", null)));
        verify(reports, never()).save(any());
    }

    @Test
    void duplicatePendingReportsAreRejected() {
        when(users.findLockedById("reporter")).thenReturn(Optional.of(reporter));
        when(posts.findById("post")).thenReturn(Optional.of(Post.builder().id("post").title("Title").status(Post.PostStatus.PUBLISHED).build()));
        when(reports.existsByReporterIdAndTargetIdAndTypeAndStatus("reporter", "post", Report.TargetType.POST, Report.ReportStatus.PENDING)).thenReturn(true);
        assertThrows(BusinessRuleException.class,
                () -> service.create("reporter", new ReportDtos.CreateRequest("post", Report.TargetType.POST, "spam", null)));
        verify(reports, never()).save(any());
    }

    @Test
    void resolvingDeleteHidesPostAndRecordsActorAndNotifiesReporter() {
        Report report = report(Report.TargetType.POST);
        Post post = Post.builder().id("target").status(Post.PostStatus.PUBLISHED).build();
        when(reports.findLockedById("report")).thenReturn(Optional.of(report));
        when(users.findById("admin")).thenReturn(Optional.of(admin));
        when(posts.findById("target")).thenReturn(Optional.of(post));

        ReportDtos.Response response = service.resolve("report", "admin", ReportDtos.Action.DELETE);

        assertEquals(Post.PostStatus.HIDDEN, post.getStatus());
        assertEquals("resolved", response.status());
        assertEquals("DELETE", response.action());
        assertEquals("admin", response.resolvedById());
        assertNotNull(response.resolvedAt());
        verify(posts).save(post);
        verify(notifications).notify(eq(reporter), eq(Notification.NotificationType.REPORT), anyString(), isNull(), eq("/notifications"));
    }

    @Test
    void alreadyResolvedReportCannotApplySecondModerationAction() {
        Report report = report(Report.TargetType.POST);
        report.setStatus(Report.ReportStatus.RESOLVED);
        when(reports.findLockedById("report")).thenReturn(Optional.of(report));
        assertThrows(BusinessRuleException.class, () -> service.resolve("report", "admin", ReportDtos.Action.DELETE));
        verifyNoInteractions(posts, users, notifications);
    }

    @Test
    void moderatorCannotBanAdministrator() {
        Report report = report(Report.TargetType.USER);
        when(reports.findLockedById("report")).thenReturn(Optional.of(report));
        when(users.findById("moderator")).thenReturn(Optional.of(User.builder().id("moderator").role(User.Role.MODERATOR).build()));
        when(users.findLockedById("target")).thenReturn(Optional.of(User.builder().id("target").role(User.Role.ADMIN).build()));
        assertThrows(ForbiddenException.class, () -> service.resolve("report", "moderator", ReportDtos.Action.BAN));
        verify(reports, never()).save(any());
        verify(users, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void outsiderCannotSubmitReportAboutPrivateGroup() {
        when(users.findLockedById("reporter")).thenReturn(Optional.of(reporter));
        when(groups.findById("group")).thenReturn(Optional.of(Group.builder().id("group").publicGroup(false).build()));
        assertThrows(ForbiddenException.class,
                () -> service.create("reporter", new ReportDtos.CreateRequest("group", Report.TargetType.GROUP, "spam", null)));
        verify(reports, never()).save(any());
    }

    private Report report(Report.TargetType type) {
        return Report.builder().id("report").targetId("target").type(type).reason("spam").reporter(reporter).build();
    }
}
