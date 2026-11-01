package com.vietblog.application.service;

import com.vietblog.application.dto.admin.AdminDtos;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.Report;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.AdminPostRepository;
import com.vietblog.infrastructure.repository.AdminUserRepository;
import com.vietblog.infrastructure.repository.CommentRepository;
import com.vietblog.infrastructure.repository.GroupRepository;
import com.vietblog.infrastructure.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final AdminUserRepository users;
    private final AdminPostRepository posts;
    private final CommentRepository comments;
    private final ReportRepository reports;
    private final GroupRepository groups;

    @Transactional(readOnly = true)
    public AdminDtos.Stats stats() {
        return stats(7);
    }

    /**
     * Thống kê dashboard theo khoảng thời gian.
     * 7/30 ngày: nhóm theo ngày; 90 ngày: theo tuần; 365 ngày: theo tháng (~30 ngày).
     */
    @Transactional(readOnly = true)
    public AdminDtos.Stats stats(int days) {
        int range = Math.max(1, Math.min(days, 365));
        int bucket = range <= 31 ? 1 : range <= 90 ? 7 : 30;
        int buckets = (int) Math.ceil(range / (double) bucket);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        var growth = new ArrayList<AdminDtos.DailyGrowth>();
        for (int i = buckets - 1; i >= 0; i--) {
            LocalDate end = today.minusDays((long) i * bucket).plusDays(1);
            LocalDate start = end.minusDays(bucket);
            growth.add(new AdminDtos.DailyGrowth(start,
                    users.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(start.atStartOfDay(), end.atStartOfDay()),
                    posts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start.atStartOfDay(), end.atStartOfDay())));
        }

        var dayStart = today.atStartOfDay();
        var dayEnd = today.plusDays(1).atStartOfDay();
        long newUsersToday = users.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(dayStart, dayEnd);
        long newPostsToday = posts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(dayStart, dayEnd);
        long usersInRange = growth.stream().mapToLong(AdminDtos.DailyGrowth::users).sum();
        long postsInRange = growth.stream().mapToLong(AdminDtos.DailyGrowth::posts).sum();

        return new AdminDtos.Stats(users.count(), newUsersToday, posts.count(), newPostsToday, comments.count(), groups.count(),
                reports.count(), reports.countByStatus(Report.ReportStatus.PENDING), posts.countTotalViews(), growth,
                usersInRange, postsInRange, topCategories(), recentActivities());
    }

    private java.util.List<AdminDtos.CategoryStat> topCategories() {
        var rows = posts.countByCategory(PageRequest.of(0, 6));
        long max = rows.stream().mapToLong(r -> ((Number) r[2]).longValue()).max().orElse(0);
        return rows.stream().map(r -> {
            long count = ((Number) r[2]).longValue();
            int pct = max == 0 ? 0 : (int) Math.round(count * 100.0 / max);
            return new AdminDtos.CategoryStat((String) r[0], (String) r[1], count, pct);
        }).toList();
    }

    private java.util.List<AdminDtos.Activity> recentActivities() {
        var list = new ArrayList<AdminDtos.Activity>();
        posts.findTop5ByOrderByCreatedAtDesc().forEach(p -> list.add(new AdminDtos.Activity("post",
                p.getAuthor().getName() + " đã đăng bài \"" + p.getTitle() + "\"", p.getCreatedAt())));
        users.findTop5ByOrderByJoinedAtDesc().forEach(u -> list.add(new AdminDtos.Activity("user",
                u.getName() + " vừa tham gia VietBlog", u.getJoinedAt())));
        reports.findTop5ByOrderByCreatedAtDesc().forEach(r -> list.add(new AdminDtos.Activity("report",
                "Báo cáo mới: " + r.getReason() + (r.getTargetTitle() != null ? " — \"" + r.getTargetTitle() + "\"" : ""),
                r.getCreatedAt())));
        list.removeIf(a -> a.time() == null);
        list.sort(java.util.Comparator.comparing(AdminDtos.Activity::time).reversed());
        return list.stream().limit(8).toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminDtos.UserResponse> users(User.Role role, String search, int page, int limit) {
        return users.search(role, search.trim(), page(page, limit, "joinedAt")).map(this::toUserResponse);
    }

    private AdminDtos.UserResponse toUserResponse(User user) {
        return AdminDtos.UserResponse.from(user, posts.countByAuthorId(user.getId()));
    }

    @Transactional(readOnly = true)
    public Page<AdminDtos.PostResponse> posts(Post.PostStatus status, String search, int page, int limit) {
        Page<Post> paged = posts.search(status, search.trim(), page(page, limit, "createdAt"));
        if (paged.isEmpty()) {
            return paged.map(AdminDtos.PostResponse::from);
        }
        var ids = paged.getContent().stream().map(Post::getId).toList();
        var counts = new java.util.HashMap<String, Long>();
        for (Object[] row : reports.countByTargetIds(Report.TargetType.POST, ids)) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return paged.map(p -> AdminDtos.PostResponse.from(p, counts.getOrDefault(p.getId(), 0L)));
    }

    @Transactional
    public AdminDtos.UserResponse changeRole(String id, String actorId, User.Role role) {
        User user = users.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (role != user.getRole() && (id.equals(actorId) || user.getRole() == User.Role.ADMIN)) {
            throw new BusinessRuleException(ErrorCode.PROTECTED_ADMIN_ACCOUNT);
        }
        user.setRole(role);
        return toUserResponse(users.save(user));
    }

    @Transactional
    public AdminDtos.UserResponse setBanned(String id, String actorId, boolean banned) {
        User user = users.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (banned && (id.equals(actorId) || user.getRole() == User.Role.ADMIN)) {
            throw new BusinessRuleException(ErrorCode.PROTECTED_ADMIN_ACCOUNT);
        }
        user.setActive(!banned);
        return toUserResponse(users.save(user));
    }

    @Transactional
    public AdminDtos.PostResponse changePostStatus(String id, Post.PostStatus status) {
        Post post = posts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        post.setStatus(status);
        return AdminDtos.PostResponse.from(posts.save(post));
    }

    @Transactional
    public void deleteUser(String id, String actorId) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (id.equals(actorId) || user.getRole() == User.Role.ADMIN) {
            throw new BusinessRuleException(ErrorCode.PROTECTED_ADMIN_ACCOUNT);
        }
        users.delete(user);
    }

    public static PageRequest page(int page, int limit, String sort) {
        if (page < 1 || limit < 1 || limit > 100) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        return PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, sort, "id"));
    }
}
