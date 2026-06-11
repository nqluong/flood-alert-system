package org.project.floodalert.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Xử lý kết quả gửi FCM thất bại, dùng chung cho FcmDispatchService và FcmDispatchWorker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmFailureHandler {

    private final DeviceTokenService deviceTokenService;

    @Value("${app.notification.retry.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    /**
     * Áp dụng kết quả thất bại lên notification.
     * Nếu Firebase trả về UNREGISTERED (token đã bị thu hồi/app gỡ cài đặt) thì token
     * không thể dùng lại được nữa: đánh dấu DEAD ngay (bỏ qua retry) và xóa token khỏi
     * DB + Redis để các lần dispatch sau không lấy phải token chết.
     * Các lỗi khác được đánh dấu FAILED và lên lịch retry như bình thường.
     */
    public void handleFailure(Notification notification, FirebaseMessagingException exception) {
        String errorCode = exception != null ? exception.getErrorCode().toString() : "UNKNOWN";
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";

        if (exception != null && exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            notification.setStatus(NotificationStatus.DEAD);
            notification.setErrorMessage(String.format("[%s] %s", errorCode, errorMessage));
            notification.setNextRetryAt(null);

            deviceTokenService.removeToken(notification.getUserId());

            log.warn("Token FCM của user {} không còn hợp lệ (UNREGISTERED), đã xóa token và đánh dấu notification {} là DEAD",
                    notification.getUserId(), notification.getId());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(String.format("[%s] %s", errorCode, errorMessage));
            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(retryDelayMinutes));

            log.warn("Gửi thất bại notification {} cho user {}: {}",
                    notification.getId(), notification.getUserId(), errorMessage);
        }
    }
}
