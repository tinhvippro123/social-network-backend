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
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String userId, boolean unreadOnly, int page, int limit) {
        if (page < 1 || limit < 1 || limit > 100) throw new com.vietblog.domain.exception.BusinessRuleException(ErrorCode.INVALID_INPUT);
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id"));
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

    @Transactional
    public void delete(String id, String userId) {
        notifications.deleteByIdAndUserId(id, userId);
    }

    /** Internal event delivery; callers must derive the recipient from a persisted domain relationship. */
    @Transactional
    public void notify(User recipient, Notification.NotificationType type, String message, String avatar, String link) {
        notifyWithPreferences(recipient, type, message, avatar, link, null, null);
    }

    /**
     * Tạo thông báo trong ứng dụng và (tùy chọn) gửi email nền dựa trên cài đặt cá nhân của người nhận.
     */
    @Transactional
    public void notifyWithPreferences(
            User recipient,
            Notification.NotificationType type,
            String message,
            String avatar,
            String link,
            String emailSubject,
            String emailHeading) {
        if (recipient == null) {
            return;
        }
        if (isInAppEnabled(recipient, type, link)) {
            notifications.save(Notification.builder()
                    .user(recipient)
                    .type(type)
                    .message(message)
                    .avatar(avatar)
                    .link(link)
                    .build());
        }
        if (emailSubject != null && isEmailEnabled(recipient, type)) {
            emailService.sendNotificationEmailAsync(
                    recipient.getEmail(),
                    recipient.getName(),
                    emailSubject,
                    emailHeading != null ? emailHeading : emailSubject,
                    message,
                    link
            );
        }
    }

    private static boolean isInAppEnabled(User recipient, Notification.NotificationType type, String link) {
        return switch (type) {
            case FOLLOW -> recipient.isNotifyNewFollowerEffective();
            case COMMENT -> recipient.isNotifyNewCommentEffective();
            case SYSTEM -> (link != null && link.startsWith("/posts/")) ? recipient.isNotifyTrendingEffective() : true;
            default -> true;
        };
    }

    private static boolean isEmailEnabled(User recipient, Notification.NotificationType type) {
        return switch (type) {
            case FOLLOW -> recipient.isEmailNewFollowerEffective();
            case COMMENT -> recipient.isEmailNewCommentEffective();
            default -> false;
        };
    }
}
