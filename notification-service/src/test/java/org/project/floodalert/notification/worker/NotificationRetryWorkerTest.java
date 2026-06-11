package org.project.floodalert.notification.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRetryWorkerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationRetryWorker worker;

    private static final int READ_RETENTION_DAYS = 30;
    private static final int FAILED_RETENTION_DAYS = 15;

    @BeforeEach
    void setUp() {
        Executor sameThreadExecutor = Runnable::run;
        worker = new NotificationRetryWorker(notificationRepository, sameThreadExecutor);
        ReflectionTestUtils.setField(worker, "readRetentionDays", READ_RETENTION_DAYS);
        ReflectionTestUtils.setField(worker, "failedRetentionDays", FAILED_RETENTION_DAYS);
    }

    @Nested
    @DisplayName("cleanupOldNotifications")
    class CleanupOldNotificationsTests {

        @Test
        @DisplayName("Xóa notification CLICKED đã đọc quá hạn (theo clickedAt)")
        void deletesOldClickedNotificationsByClickedAt() {
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            when(notificationRepository.deleteByStatusAndClickedAtBefore(eq(NotificationStatus.CLICKED), cutoffCaptor.capture()))
                    .thenReturn(3);
            when(notificationRepository.deleteByStatusInAndCreatedAtBefore(anyList(), any())).thenReturn(0);

            worker.cleanupOldNotifications();

            LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(READ_RETENTION_DAYS);
            assertThat(cutoffCaptor.getValue()).isCloseTo(expectedCutoff, within(2, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Xóa notification FAILED/DEAD gửi lỗi quá hạn (theo createdAt)")
        void deletesOldFailedAndDeadNotificationsByCreatedAt() {
            when(notificationRepository.deleteByStatusAndClickedAtBefore(any(), any())).thenReturn(0);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            when(notificationRepository.deleteByStatusInAndCreatedAtBefore(statusCaptor.capture(), cutoffCaptor.capture()))
                    .thenReturn(5);

            worker.cleanupOldNotifications();

            assertThat(statusCaptor.getValue())
                    .containsExactlyInAnyOrder(NotificationStatus.FAILED, NotificationStatus.DEAD);
            LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(FAILED_RETENTION_DAYS);
            assertThat(cutoffCaptor.getValue()).isCloseTo(expectedCutoff, within(2, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Không ném exception ra ngoài khi repository lỗi")
        void doesNotThrowWhenRepositoryFails() {
            when(notificationRepository.deleteByStatusAndClickedAtBefore(any(), any()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatCode(() -> worker.cleanupOldNotifications()).doesNotThrowAnyException();
        }
    }
}
