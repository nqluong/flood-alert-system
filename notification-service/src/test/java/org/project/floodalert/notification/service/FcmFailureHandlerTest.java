package org.project.floodalert.notification.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmFailureHandlerTest {

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private FirebaseMessagingException exception;

    private FcmFailureHandler handler;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new FcmFailureHandler(deviceTokenService);
        ReflectionTestUtils.setField(handler, "retryDelayMinutes", 5);
    }

    private Notification notificationWithRetryCount(int retryCount) {
        return Notification.builder()
                .userId(USER_ID)
                .status(NotificationStatus.PENDING)
                .retryCount(retryCount)
                .build();
    }

    @Test
    @DisplayName("UNREGISTERED -> đánh dấu DEAD, không retry và xóa FCM token")
    void unregisteredTokenMarksDeadAndRemovesToken() {
        when(exception.getErrorCode()).thenReturn(ErrorCode.NOT_FOUND);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(exception.getMessage()).thenReturn("Requested entity was not found.");

        Notification notification = notificationWithRetryCount(0);

        handler.handleFailure(notification, exception);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD);
        assertThat(notification.getNextRetryAt()).isNull();
        assertThat(notification.getRetryCount()).isZero();
        assertThat(notification.getErrorMessage()).contains("Requested entity was not found.");
        verify(deviceTokenService).removeToken(USER_ID);
    }

    @Test
    @DisplayName("Lỗi khác UNREGISTERED -> đánh dấu FAILED, tăng retryCount và lên lịch retry")
    void otherErrorMarksFailedAndSchedulesRetry() {
        when(exception.getErrorCode()).thenReturn(ErrorCode.UNAVAILABLE);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(exception.getMessage()).thenReturn("Server unavailable");

        Notification notification = notificationWithRetryCount(1);

        handler.handleFailure(notification, exception);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getRetryCount()).isEqualTo(2);
        assertThat(notification.getErrorMessage()).contains("Server unavailable");
        assertThat(notification.getNextRetryAt()).isAfter(LocalDateTime.now());
        verify(deviceTokenService, never()).removeToken(any());
    }

    @Test
    @DisplayName("Exception null -> đánh dấu FAILED với message Unknown error")
    void nullExceptionMarksFailedWithUnknownError() {
        Notification notification = notificationWithRetryCount(0);

        handler.handleFailure(notification, null);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getErrorMessage()).contains("Unknown error");
        verify(deviceTokenService, never()).removeToken(any());
    }
}
