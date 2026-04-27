package org.project.floodalert.notification.service;

import org.project.floodalert.notification.dto.response.NotificationListResponse;
import org.project.floodalert.notification.dto.response.UnreadCountResponse;

import java.util.UUID;

public interface UserNotificationService {

    NotificationListResponse getUserNotifications(UUID userId, int page, int size);

    UnreadCountResponse getUnreadCount(UUID userId);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);
}
