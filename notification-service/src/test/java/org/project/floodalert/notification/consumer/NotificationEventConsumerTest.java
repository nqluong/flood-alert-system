package org.project.floodalert.notification.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.notification.dto.event.FloodLifecycleEvent;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationContext;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.service.FcmDispatchService;
import org.project.floodalert.notification.service.NotificationAggregationService;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock private NotificationAggregationService aggregationService;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private FcmDispatchService fcmDispatchService;
    @Mock private Acknowledgment acknowledgment;

    @InjectMocks
    private NotificationEventConsumer consumer;

    private static final UUID USER_ID = UUID.randomUUID();

    private FloodLifecycleEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = FloodLifecycleEvent.builder()
                .eventId("event-123")
                .type("FLOOD_DETECTED")
                .waterLevel(25.0)
                .severityLevel("DANGER")
                .location("Test Location")
                .lat(10.762622)
                .lon(106.660172)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NotificationContext contextFor(UUID userId) {
        return NotificationContext.builder()
                .userId(userId)
                .isNearActive(true)
                .activeDistance(300.0)
                .build();
    }

    private NotificationPreference prefWith(UUID userId, boolean enabled, boolean floodAlerts, boolean preferPush) {
        return NotificationPreference.builder()
                .userId(userId)
                .enabled(enabled)
                .floodAlerts(floodAlerts)
                .preferPush(preferPush)
                .quietHoursEnabled(false)
                .alertRadiusMeters(500)
                .build();
    }

    private void stubAggregation(UUID... userIds) {
        Map<UUID, NotificationContext> ctx = new LinkedHashMap<>();
        for (UUID id : userIds) ctx.put(id, contextFor(id));
        when(aggregationService.aggregateNotificationContexts(any(), anyBoolean())).thenReturn(ctx);
        when(aggregationService.generateNotificationTitle(any())).thenReturn("Ngập lụt gần vị trí của bạn");
        when(aggregationService.generateNotificationBody(any(), any())).thenReturn("Có điểm ngập cách 300m (mức độ: Nguy hiểm)");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Event Processing Flow")
    class EventFlowTests {

        @Test
        @DisplayName("Skip khi aggregation không tìm thấy user nào")
        void skipWhenNoAggregatedContexts() {
            when(aggregationService.aggregateNotificationContexts(any(), anyBoolean()))
                    .thenReturn(Collections.emptyMap());

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(preferenceRepository, never()).findAllById(any());
            verify(fcmDispatchService, never()).sendBatch(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Skip khi tất cả user bị lọc bởi preferences")
        void skipWhenAllUsersFiltered() {
            stubAggregation(USER_ID);
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, false, true, true)));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Gửi notification cho user hợp lệ và acknowledge")
        void sendNotificationsForValidUsers() {
            stubAggregation(USER_ID);
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, true, true)));
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService).sendBatch(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("User không có record preference → được phép nhận (default allow)")
        void allowUserWithNoPreferenceRecord() {
            stubAggregation(USER_ID);
            when(preferenceRepository.findAllById(any())).thenReturn(Collections.emptyList());
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService).sendBatch(any());
        }
    }

    @Nested
    @DisplayName("Preference Filter Logic")
    class PreferenceFilterTests {

        @BeforeEach
        void setupContext() {
            stubAggregation(USER_ID);
        }

        @Test
        @DisplayName("Block khi enabled=false")
        void blockWhenDisabled() {
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, false, true, true)));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
        }

        @Test
        @DisplayName("Block khi preferPush=false")
        void blockWhenPreferPushFalse() {
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, true, false)));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
        }

        @Test
        @DisplayName("Block khi floodAlerts=false (severeOnly) + severity LOW")
        void blockWhenSevereOnlyAndLow() {
            testEvent.setSeverityLevel("LOW");
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, false, true)));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
        }

        @Test
        @DisplayName("Block khi floodAlerts=false (severeOnly) + severity MEDIUM")
        void blockWhenSevereOnlyAndMedium() {
            testEvent.setSeverityLevel("MEDIUM");
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, false, true)));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
        }

        @Test
        @DisplayName("Block khi floodAlerts=false (severeOnly) + severity WARNING (IoT)")
        void blockWhenSevereOnlyAndWarning() {
            testEvent.setSeverityLevel("WARNING");
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, false, true)));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
        }

        @Test
        @DisplayName("Allow khi floodAlerts=false (severeOnly) + severity DANGER")
        void allowWhenSevereOnlyAndDanger() {
            testEvent.setSeverityLevel("DANGER");
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, false, true)));
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService).sendBatch(any());
        }

        @Test
        @DisplayName("Allow khi floodAlerts=false (severeOnly) + severity CRITICAL")
        void allowWhenSevereOnlyAndCritical() {
            testEvent.setSeverityLevel("CRITICAL");
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, false, true)));
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService).sendBatch(any());
        }

        @Test
        @DisplayName("Block khi đang trong quiet hours")
        void blockWhenInQuietHours() {
            LocalTime now = LocalTime.now();
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(USER_ID)
                    .enabled(true).floodAlerts(true).preferPush(true)
                    .quietHoursEnabled(true)
                    .quietHoursStart(now.minusHours(1))
                    .quietHoursEnd(now.plusHours(1))
                    .build();
            when(preferenceRepository.findAllById(any())).thenReturn(List.of(pref));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService, never()).sendBatch(any());
        }

        @Test
        @DisplayName("Filter đúng khi có nhiều user với preferences khác nhau")
        void filterMixedUsersCorrectly() {
            UUID user2 = UUID.randomUUID();
            UUID user3 = UUID.randomUUID();
            stubAggregation(USER_ID, user2, user3);

            List<NotificationPreference> prefs = List.of(
                    prefWith(USER_ID, true, true, true),       // allowed
                    prefWith(user2, false, true, true),         // blocked (disabled)
                    prefWith(user3, true, true, true)           // allowed
            );
            when(preferenceRepository.findAllById(any())).thenReturn(prefs);
            when(fcmDispatchService.sendBatch(any())).thenReturn(2);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue())
                    .extracting(Notification::getUserId)
                    .containsExactlyInAnyOrder(USER_ID, user3);
        }
    }

    @Nested
    @DisplayName("Notification Priority")
    class PriorityTests {

        @Captor
        ArgumentCaptor<List<Notification>> captor;

        @BeforeEach
        void setupContext() {
            stubAggregation(USER_ID);
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, true, true)));
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);
        }

        @Test
        @DisplayName("CRITICAL → HIGH priority")
        void criticalIsHighPriority() {
            testEvent.setSeverityLevel("CRITICAL");
            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue().get(0).getPriority()).isEqualTo(NotificationPriority.HIGH);
        }

        @Test
        @DisplayName("DANGER → HIGH priority")
        void dangerIsHighPriority() {
            testEvent.setSeverityLevel("DANGER");
            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue().get(0).getPriority()).isEqualTo(NotificationPriority.HIGH);
        }

        @Test
        @DisplayName("MEDIUM → NORMAL priority")
        void mediumIsNormalPriority() {
            testEvent.setSeverityLevel("MEDIUM");
            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue().get(0).getPriority()).isEqualTo(NotificationPriority.NORMAL);
        }

        @Test
        @DisplayName("WARNING (IoT FloodStatus) → NORMAL priority")
        void warningIsNormalPriority() {
            testEvent.setSeverityLevel("WARNING");
            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue().get(0).getPriority()).isEqualTo(NotificationPriority.NORMAL);
        }

        @Test
        @DisplayName("LOW → LOW priority")
        void lowIsLowPriority() {
            testEvent.setSeverityLevel("LOW");
            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue().get(0).getPriority()).isEqualTo(NotificationPriority.LOW);
        }

        @Test
        @DisplayName("null severity → NORMAL priority")
        void nullSeverityIsNormalPriority() {
            testEvent.setSeverityLevel(null);
            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);
            verify(fcmDispatchService).sendBatch(captor.capture());
            assertThat(captor.getValue().get(0).getPriority()).isEqualTo(NotificationPriority.NORMAL);
        }
    }

    @Nested
    @DisplayName("Notification Content")
    class ContentTests {

        @Captor
        ArgumentCaptor<List<Notification>> captor;

        @Test
        @DisplayName("Notification có đúng type, status, channel và data payload")
        void notificationHasCorrectStructure() {
            stubAggregation(USER_ID);
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, true, true)));
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService).sendBatch(captor.capture());
            Notification n = captor.getValue().get(0);

            assertThat(n.getNotificationType()).isEqualTo("FLOOD_ALERT");
            assertThat(n.getUserId()).isEqualTo(USER_ID);
            assertThat(n.getData()).containsKey("eventId");
            assertThat(n.getData()).containsKey("lat");
            assertThat(n.getData()).containsKey("lon");
            assertThat(n.getData()).containsKey("waterLevel");
            assertThat(n.getData()).containsKey("severityLevel");
        }

        @Test
        @DisplayName("RESOLVED event → type FLOOD_RESOLVED và title đúng")
        void resolvedEventHasCorrectType() {
            testEvent.setType("RESOLVED");
            stubAggregation(USER_ID);
            when(aggregationService.generateResolvedTitle()).thenReturn("Khu vực đã hết ngập");
            when(aggregationService.generateResolvedBody(any(), any())).thenReturn("Bạn có thể di chuyển bình thường.");
            when(preferenceRepository.findAllById(any()))
                    .thenReturn(List.of(prefWith(USER_ID, true, true, true)));
            when(fcmDispatchService.sendBatch(any())).thenReturn(1);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(fcmDispatchService).sendBatch(captor.capture());
            Notification n = captor.getValue().get(0);
            assertThat(n.getNotificationType()).isEqualTo("FLOOD_RESOLVED");
            assertThat(n.getTitle()).isEqualTo("Khu vực đã hết ngập");
        }
    }
}
