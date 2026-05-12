package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.response.NotificationListResponse;
import org.project.floodalert.notification.dto.response.NotificationResponse;
import org.project.floodalert.notification.dto.response.UnreadCountResponse;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.project.floodalert.notification.service.UserNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getUserNotifications(UUID userId, int page, int size) {
        log.debug("[UserNotificationService] Lấy danh sách thông báo: userId={}, page={}, size={}",
                userId, page, size);

        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());

        long unreadCount = notificationRepository.countUnreadByUserId(userId);

        log.debug("[UserNotificationService] Tìm thấy {} thông báo, {} chưa đọc",
                notifications.size(), unreadCount);

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .currentPage(notificationPage.getNumber())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        log.debug("[UserNotificationService] Đếm thông báo chưa đọc: userId={}", userId);
        
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        
        log.debug("[UserNotificationService] userId={} có {} thông báo chưa đọc", userId, unreadCount);
        
        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        log.debug("[UserNotificationService] Đánh dấu đã đọc: userId={}, notificationId={}",
                userId, notificationId);

        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        
        if (updated > 0) {
            log.debug("[UserNotificationService] Đã đánh dấu đọc thông báo: {}", notificationId);
        } else {
            log.warn("[UserNotificationService] Không tìm thấy thông báo hoặc đã đọc rồi: {}", notificationId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        log.debug("[UserNotificationService] Đánh dấu tất cả đã đọc: userId={}", userId);

        int updated = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        
        log.debug("[UserNotificationService] Đã đánh dấu đọc {} thông báo cho userId={}", updated, userId);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .notificationType(notification.getNotificationType())
                .priority(notification.getPriority() != null ? notification.getPriority().name() : null)
                .data(notification.getData())
                .isRead(notification.getClickedAt() != null)
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .clickedAt(notification.getClickedAt())
                .build();
    }
}
