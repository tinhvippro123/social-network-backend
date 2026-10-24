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
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        var growth = new ArrayList<AdminDtos.DailyGrowth>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            growth.add(new AdminDtos.DailyGrowth(date,
                    users.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(date.atStartOfDay(), date.plusDays(1).atStartOfDay()),
                    posts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(date.atStartOfDay(), date.plusDays(1).atStartOfDay())));
        }
        AdminDtos.DailyGrowth current = growth.get(growth.size() - 1);
        return new AdminDtos.Stats(users.count(), current.users(), posts.count(), current.posts(), comments.count(), groups.count(),
                reports.count(), reports.countByStatus(Report.ReportStatus.PENDING), posts.countTotalViews(), growth);
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
        return posts.search(status, search.trim(), page(page, limit, "createdAt")).map(AdminDtos.PostResponse::from);
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
