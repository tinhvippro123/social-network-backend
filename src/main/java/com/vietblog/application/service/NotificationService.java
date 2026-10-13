package com.vietblog.application.service;

import com.vietblog.application.dto.notification.NotificationResponse;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String userId, boolean unreadOnly, int page, int limit) {
        var pageable = AdminService.page(page, limit, "createdAt");
        return (unreadOnly ? notifications.findByUserIdAndIsReadFalse(userId, pageable)
                : notifications.findByUserId(userId, pageable)).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return notifications.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(String id, String userId) {
        if (notifications.markRead(id, userId) == 0) {
            throw new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Transactional
    public int markAllRead(String userId) {
        return notifications.markAllRead(userId);
    }

    /** Internal event delivery; callers must derive the recipient from a persisted domain relationship. */
    @Transactional
    public void notify(User recipient, Notification.NotificationType type, String message, String avatar, String link) {
        notifications.save(Notification.builder().user(recipient).type(type).message(message).avatar(avatar).link(link).build());
    }
}
