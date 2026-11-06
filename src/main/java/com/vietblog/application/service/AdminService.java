package com.vietblog.application.service;

import com.vietblog.application.dto.admin.AdminDtos;
import com.vietblog.domain.entity.Group;
import com.vietblog.domain.entity.GroupMember;
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
import com.vietblog.infrastructure.repository.GroupRepository;
import com.vietblog.infrastructure.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final AdminUserRepository users;
    private final AdminPostRepository posts;
    private final CommentRepository comments;
    private final ReportRepository reports;
    private final GroupRepository groups;
    private final PostApplicationService postApplicationService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public AdminDtos.Stats stats() {
        return stats(7);
    }

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

    private List<AdminDtos.CategoryStat> topCategories() {
        var rows = posts.countByCategory(PageRequest.of(0, 6));
        long max = rows.stream().mapToLong(r -> ((Number) r[2]).longValue()).max().orElse(0);
        return rows.stream().map(r -> {
            long count = ((Number) r[2]).longValue();
            int pct = max == 0 ? 0 : (int) Math.round(count * 100.0 / max);
            return new AdminDtos.CategoryStat((String) r[0], (String) r[1], count, pct);
        }).toList();
    }

    private List<AdminDtos.Activity> recentActivities() {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public AdminDtos.PostResponse changePostStatus(String id, Post.PostStatus status) {
        if (status == Post.PostStatus.DRAFT) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
        Post post = posts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        if (post.getStatus() == Post.PostStatus.DRAFT) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        post.setStatus(status);
        Post saved = posts.saveAndFlush(post);
        postApplicationService.syncAuthorAndCategoryCounters(saved.getAuthor(), saved.getCategory());
        long reportCount = reports != null
                ? reports.countByTargetIds(Report.TargetType.POST, List.of(saved.getId())).stream()
                        .mapToLong(r -> ((Number) r[1]).longValue()).findFirst().orElse(0L)
                : 0L;
        return AdminDtos.PostResponse.from(saved, reportCount);
    }

    @Transactional
    public void deleteUser(String id, String actorId) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (id.equals(actorId) || user.getRole() == User.Role.ADMIN) {
            throw new BusinessRuleException(ErrorCode.PROTECTED_ADMIN_ACCOUNT);
        }
        User actor = users.findById(actorId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 1. Clear followers / following join table & sync peer counters
        for (User follower : new ArrayList<>(user.getFollowers())) {
            follower.getFollowing().remove(user);
            follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
            users.save(follower);
        }
        for (User followed : new ArrayList<>(user.getFollowing())) {
            followed.getFollowers().remove(user);
            followed.setFollowersCount(Math.max(0, followed.getFollowersCount() - 1));
            users.save(followed);
        }
        user.getFollowers().clear();
        user.getFollowing().clear();
        users.saveAndFlush(user);

        // 2. Delete tokens, blocks, notifications, reports
        em.createQuery("DELETE FROM PasswordResetToken t WHERE t.user.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM ChatBlock b WHERE b.blocker.id = :uid OR b.blocked.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM Notification n WHERE n.user.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("UPDATE Report r SET r.resolvedBy = null WHERE r.resolvedBy.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM Report r WHERE r.reporter.id = :uid").setParameter("uid", id).executeUpdate();

        // 3. Clean up chat messages, memberships & reassign created conversations to actor
        em.createQuery("UPDATE ChatMessage m SET m.replyTo = null WHERE m.replyTo.id IN (SELECT r.id FROM ChatMessage r WHERE r.sender.id = :uid)").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM ChatMessage m WHERE m.sender.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM ConversationMember cm WHERE cm.user.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("UPDATE Conversation c SET c.creator = :actor WHERE c.creator.id = :uid")
                .setParameter("actor", actor).setParameter("uid", id).executeUpdate();

        // 4. Clean up group interactions & reassign owned groups to actor
        em.createQuery("DELETE FROM GroupPostLike l WHERE l.user.id = :uid OR l.post.id IN (SELECT p.id FROM GroupPost p WHERE p.author.id = :uid)").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM GroupPostComment c WHERE c.author.id = :uid OR c.post.id IN (SELECT p.id FROM GroupPost p WHERE p.author.id = :uid)").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM GroupPost p WHERE p.author.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM GroupMember gm WHERE gm.user.id = :uid").setParameter("uid", id).executeUpdate();

        List<Group> ownedGroups = em.createQuery("SELECT g FROM CommunityGroup g WHERE g.owner.id = :uid", Group.class)
                .setParameter("uid", id).getResultList();
        for (Group g : ownedGroups) {
            g.setOwner(actor);
            Long existingMembership = em.createQuery(
                    "SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :gid AND gm.user.id = :aid", Long.class)
                    .setParameter("gid", g.getId()).setParameter("aid", actorId).getSingleResult();
            if (existingMembership == null || existingMembership == 0) {
                em.persist(GroupMember.builder().group(g).user(actor).role(GroupMember.Role.OWNER).build());
            } else {
                em.createQuery("UPDATE GroupMember gm SET gm.role = :role WHERE gm.group.id = :gid AND gm.user.id = :aid")
                        .setParameter("role", GroupMember.Role.OWNER)
                        .setParameter("gid", g.getId())
                        .setParameter("aid", actorId)
                        .executeUpdate();
            }
        }

        // 5. Clean up reactions, bookmarks, comments, and posts authored by user
        List<String> affectedPostIds = new ArrayList<>();
        affectedPostIds.addAll(em.createQuery("SELECT DISTINCT r.post.id FROM Reaction r WHERE r.user.id = :uid", String.class)
                .setParameter("uid", id).getResultList());
        affectedPostIds.addAll(em.createQuery("SELECT DISTINCT c.post.id FROM Comment c WHERE c.author.id = :uid", String.class)
                .setParameter("uid", id).getResultList());

        List<String> upvotedPostIds = em.createQuery(
                "SELECT r.post.id FROM Reaction r WHERE r.user.id = :uid AND r.emoji = 'UPVOTE'", String.class)
                .setParameter("uid", id).getResultList();
        List<String> downvotedPostIds = em.createQuery(
                "SELECT r.post.id FROM Reaction r WHERE r.user.id = :uid AND r.emoji = 'DOWNVOTE'", String.class)
                .setParameter("uid", id).getResultList();

        em.createQuery("DELETE FROM Reaction r WHERE r.user.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM Bookmark b WHERE b.user.id = :uid").setParameter("uid", id).executeUpdate();
        em.createQuery("UPDATE Comment c SET c.parent = null WHERE c.parent.id IN (SELECT p.id FROM Comment p WHERE p.author.id = :uid)").setParameter("uid", id).executeUpdate();
        em.createQuery("DELETE FROM Comment c WHERE c.author.id = :uid").setParameter("uid", id).executeUpdate();

        var authoredPostIds = em.createQuery("SELECT p.id FROM Post p WHERE p.author.id = :uid", String.class)
                .setParameter("uid", id).getResultList();
        for (String postId : authoredPostIds) {
            postApplicationService.deletePost(postId, actorId);
        }

        for (String pid : affectedPostIds.stream().distinct().toList()) {
            if (!authoredPostIds.contains(pid)) {
                posts.findById(pid).ifPresent(p -> {
                    long commentCount = comments.countByPostId(pid);
                    p.setCommentsCount(Math.toIntExact(commentCount));
                    if (upvotedPostIds.contains(pid)) {
                        p.setUpvotesCount(Math.max(0, p.getUpvotesCount() - 1));
                    }
                    if (downvotedPostIds.contains(pid)) {
                        p.setDownvotesCount(Math.max(0, p.getDownvotesCount() - 1));
                    }
                    posts.save(p);
                });
            }
        }

        em.flush();
        users.delete(user);
    }

    public static PageRequest page(int page, int limit, String sort) {
        if (page < 1 || limit < 1 || limit > 100) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        return PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, sort, "id"));
    }
}
