package org.project.floodalert.notification.controller;

import com.google.protobuf.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.InternalUserDetails;
import org.project.floodalert.notification.dto.response.NotificationListResponse;
import org.project.floodalert.notification.dto.response.UnreadCountResponse;
import org.project.floodalert.notification.service.UserNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    /**
     * Lấy danh sách thông báo của user hiện tại
     * 
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng thông báo mỗi trang (default 20, max 100)
     * @return Danh sách thông báo với pagination info
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        UUID userId = extractUserId(authentication);
        log.info("[UserNotificationController] GET /api/v1/notifications - userId={}, page={}, size={}", 
                userId, page, size);

        NotificationListResponse response = userNotificationService.getUserNotifications(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Đếm số thông báo chưa đọc
     * 
     * @return Số lượng thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("[UserNotificationController] GET /api/v1/notifications/unread-count - userId={}", userId);

        UnreadCountResponse response = userNotificationService.getUnreadCount(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Đánh dấu một thông báo đã đọc
     * 
     * @param notificationId ID của thông báo
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable(name = "notificationId") UUID notificationId) {
        
        UUID userId = extractUserId(authentication);
        log.info("[UserNotificationController] PUT /api/v1/notifications/{}/read - userId={}", 
                notificationId, userId);

        userNotificationService.markAsRead(userId, notificationId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Notification marked as read")
                .build());
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("[UserNotificationController] PUT /api/v1/notifications/read-all - userId={}", userId);

        userNotificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("All notifications marked as read")
                .build());
    }


    private UUID extractUserId(Authentication authentication) {
        return UUID.fromString(((InternalUserDetails) authentication.getPrincipal()).getUserId());
    }
}
