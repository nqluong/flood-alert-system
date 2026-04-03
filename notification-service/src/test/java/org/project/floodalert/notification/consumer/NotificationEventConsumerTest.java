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
import org.project.floodalert.notification.dto.UserGeoDTO;
import org.project.floodalert.notification.dto.event.FloodLifecycleEvent;
import org.project.floodalert.notification.enums.NotificationChannel;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.repository.NotificationRepository;
import org.project.floodalert.notification.service.RedisGeoService;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private RedisGeoService redisGeoService;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationCaptor;

    private FloodLifecycleEvent testEvent;
    private UUID userId1;
    private UUID userId2;
    private UUID userId3;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        testEvent = FloodLifecycleEvent.builder()
                .eventId("event-123")
                .type("FLOOD_DETECTED")
                .waterLevel(1.5)
                .severityLevel("HIGH")
                .location("Test Location")
                .lat(10.762622)
                .lon(106.660172)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("consumeFloodLifecycleEvent Tests")
    class ConsumeFloodLifecycleEventTests {

        @Test
        @DisplayName("Should acknowledge and skip when no users found in radar")
        void shouldSkipWhenNoUsersInRadar() {
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(Collections.emptyList());

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(redisGeoService).findUsersNear(testEvent.getLat(), testEvent.getLon(), 5.0);
            verify(preferenceRepository, never()).findAllById(any());
            verify(notificationRepository, never()).saveAll(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should acknowledge and skip when all users filtered out")
        void shouldSkipWhenAllUsersFilteredOut() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId1)
                    .enabled(false)
                    .build();
            when(preferenceRepository.findAllById(anySet())).thenReturn(List.of(pref));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository, never()).saveAll(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should save notifications for valid users and acknowledge")
        void shouldSaveNotificationsForValidUsers() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build(),
                    UserGeoDTO.builder().userId(userId2).distance(400.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            List<NotificationPreference> prefs = List.of(
                    createValidPreference(userId1, 500),
                    createValidPreference(userId2, 500)
            );
            when(preferenceRepository.findAllById(anySet())).thenReturn(prefs);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            List<Notification> savedNotifications = notificationCaptor.getValue();

            assertThat(savedNotifications).hasSize(2);
            assertThat(savedNotifications).allMatch(n -> n.getStatus() == NotificationStatus.PENDING);
            assertThat(savedNotifications).allMatch(n -> n.getChannel() == NotificationChannel.PUSH);
            verify(acknowledgment).acknowledge();
        }
    }

    @Nested
    @DisplayName("Filter Through Funnel Tests")
    class FilterThroughFunnelTests {

        @Test
        @DisplayName("Should allow user with default preference when distance <= 500m")
        void shouldAllowUserWithDefaultPreferenceWhenNearby() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);
            when(preferenceRepository.findAllById(anySet())).thenReturn(Collections.emptyList());

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("Should block user with default preference when distance > 500m")
        void shouldBlockUserWithDefaultPreferenceWhenFar() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(600.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);
            when(preferenceRepository.findAllById(anySet())).thenReturn(Collections.emptyList());

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should block user when enabled=false")
        void shouldBlockUserWhenDisabled() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId1)
                    .enabled(false)
                    .floodAlerts(true)
                    .preferPush(true)
                    .alertRadiusMeters(500)
                    .build();
            when(preferenceRepository.findAllById(anySet())).thenReturn(List.of(pref));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should block user when floodAlerts=false")
        void shouldBlockUserWhenFloodAlertsDisabled() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId1)
                    .enabled(true)
                    .floodAlerts(false)
                    .preferPush(true)
                    .alertRadiusMeters(500)
                    .build();
            when(preferenceRepository.findAllById(anySet())).thenReturn(List.of(pref));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should block user when preferPush=false")
        void shouldBlockUserWhenPreferPushDisabled() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId1)
                    .enabled(true)
                    .floodAlerts(true)
                    .preferPush(false)
                    .alertRadiusMeters(500)
                    .build();
            when(preferenceRepository.findAllById(anySet())).thenReturn(List.of(pref));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should block user when distance > alertRadiusMeters")
        void shouldBlockUserWhenOutsideAlertRadius() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(600.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            NotificationPreference pref = createValidPreference(userId1, 500);
            when(preferenceRepository.findAllById(anySet())).thenReturn(List.of(pref));

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should filter mixed users correctly")
        void shouldFilterMixedUsersCorrectly() {
            List<UserGeoDTO> nearbyUsers = List.of(
                    UserGeoDTO.builder().userId(userId1).distance(300.0).build(),
                    UserGeoDTO.builder().userId(userId2).distance(300.0).build(),
                    UserGeoDTO.builder().userId(userId3).distance(300.0).build()
            );
            when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(nearbyUsers);

            List<NotificationPreference> prefs = List.of(
                    createValidPreference(userId1, 500),
                    NotificationPreference.builder().userId(userId2).enabled(false).build(),
                    createValidPreference(userId3, 500)
            );
            when(preferenceRepository.findAllById(anySet())).thenReturn(prefs);

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue()).hasSize(2);
            assertThat(notificationCaptor.getValue()).extracting(Notification::getUserId)
                    .containsExactlyInAnyOrder(userId1, userId3);
        }
    }

    @Nested
    @DisplayName("Notification Priority Tests")
    class NotificationPriorityTests {

        @Test
        @DisplayName("Should set HIGH priority for CRITICAL severity")
        void shouldSetHighPriorityForCritical() {
            testEvent.setSeverityLevel("CRITICAL");
            setupValidUserScenario();

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().get(0).getPriority())
                    .isEqualTo(NotificationPriority.HIGH);
        }

        @Test
        @DisplayName("Should set NORMAL priority for MEDIUM severity")
        void shouldSetNormalPriorityForMedium() {
            testEvent.setSeverityLevel("MEDIUM");
            setupValidUserScenario();

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().get(0).getPriority())
                    .isEqualTo(NotificationPriority.NORMAL);
        }

        @Test
        @DisplayName("Should set LOW priority for unknown severity")
        void shouldSetLowPriorityForUnknown() {
            testEvent.setSeverityLevel("UNKNOWN");
            setupValidUserScenario();

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().get(0).getPriority())
                    .isEqualTo(NotificationPriority.LOW);
        }

        @Test
        @DisplayName("Should set NORMAL priority for null severity")
        void shouldSetNormalPriorityForNull() {
            testEvent.setSeverityLevel(null);
            setupValidUserScenario();

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().get(0).getPriority())
                    .isEqualTo(NotificationPriority.NORMAL);
        }
    }

    @Nested
    @DisplayName("Notification Content Tests")
    class NotificationContentTests {

        @Test
        @DisplayName("Should build notification with correct content")
        void shouldBuildNotificationWithCorrectContent() {
            setupValidUserScenario();

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            Notification notification = notificationCaptor.getValue().get(0);

            assertThat(notification.getTitle()).isEqualTo("Cảnh báo ngập lụt");
            assertThat(notification.getBody()).contains("300");
            assertThat(notification.getNotificationType()).isEqualTo("FLOOD_ALERT");
            assertThat(notification.getFcmToken()).isNull();
        }

        @Test
        @DisplayName("Should build notification data map correctly")
        void shouldBuildNotificationDataCorrectly() {
            setupValidUserScenario();

            consumer.consumeFloodLifecycleEvent(testEvent, acknowledgment);

            verify(notificationRepository).saveAll(notificationCaptor.capture());
            Map<String, Object> data = notificationCaptor.getValue().get(0).getData();

            assertThat(data).containsEntry("eventId", testEvent.getEventId());
            assertThat(data).containsEntry("type", testEvent.getType());
            assertThat(data).containsEntry("waterLevel", testEvent.getWaterLevel());
            assertThat(data).containsKey("timestamp");
        }
    }

    private NotificationPreference createValidPreference(UUID userId, int alertRadius) {
        return NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .floodAlerts(true)
                .preferPush(true)
                .alertRadiusMeters(alertRadius)
                .quietHoursEnabled(false)
                .build();
    }

    private void setupValidUserScenario() {
        List<UserGeoDTO> nearbyUsers = List.of(
                UserGeoDTO.builder().userId(userId1).distance(300.0).build()
        );
        when(redisGeoService.findUsersNear(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(nearbyUsers);

        NotificationPreference pref = createValidPreference(userId1, 500);
        when(preferenceRepository.findAllById(anySet())).thenReturn(List.of(pref));
    }
}
