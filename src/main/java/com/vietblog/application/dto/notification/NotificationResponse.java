package com.vietblog.application.dto.notification;

import com.vietblog.domain.entity.Notification;
import java.time.LocalDateTime;
import java.util.Locale;

public record NotificationResponse(String id, String type, String message, String avatar,
                                   String link, boolean isRead, LocalDateTime createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType().name().toLowerCase(Locale.ROOT),
                notification.getMessage(), notification.getAvatar(), notification.getLink(),
                notification.isRead(), notification.getCreatedAt());
    }
}
