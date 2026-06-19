package org.project.floodalert.notification.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.notification.dto.event.ReputationUpdateEvent;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.service.FcmDispatchService;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationEventConsumerTest {

    @Mock
    private FcmDispatchService fcmDispatchService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private ReputationEventConsumer consumer;

    @SuppressWarnings("unchecked")
    private Notification captureSentNotification() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(fcmDispatchService).sendBatch(captor.capture());
        return captor.getValue().get(0);
    }

    @Test
    void approvedEvent_buildsNotificationWithLocationData() {
        ReputationUpdateEvent event = ReputationUpdateEvent.builder()
                .userId(UUID.randomUUID())
                .eventId("EVT-1")
                .reason("CLUSTER_APPROVED")
                .points(5)
                .reportId("REPORT-1")
                .lat(10.7626)
                .lon(106.6602)
                .address("Quận 5, TP.HCM")
                .timestamp(LocalDateTime.now())
                .build();
        when(fcmDispatchService.sendBatch(anyList())).thenReturn(1);

        consumer.consumeReputationUpdateEvent(event, ack);

        Notification sent = captureSentNotification();
        assertThat(sent.getNotificationType()).isEqualTo("REPORT_APPROVED");
        assertThat(sent.getData())
                .containsEntry("lat", 10.7626)
                .containsEntry("lon", 106.6602)
                .containsEntry("location", "Quận 5, TP.HCM");
        verify(ack).acknowledge();
    }

    @Test
    void approvedEvent_withoutLocation_omitsLocationKeys() {
        ReputationUpdateEvent event = ReputationUpdateEvent.builder()
                .userId(UUID.randomUUID())
                .eventId("EVT-2")
                .reason("CLUSTER_APPROVED")
                .points(5)
                .reportId("REPORT-2")
                .timestamp(LocalDateTime.now())
                .build();
        when(fcmDispatchService.sendBatch(anyList())).thenReturn(1);

        consumer.consumeReputationUpdateEvent(event, ack);

        Notification sent = captureSentNotification();
        assertThat(sent.getData())
                .doesNotContainKeys("lat", "lon", "location");
    }

    @Test
    void unknownReason_isSkipped() {
        ReputationUpdateEvent event = ReputationUpdateEvent.builder()
                .userId(UUID.randomUUID())
                .reason("SOMETHING_ELSE")
                .points(1)
                .build();

        consumer.consumeReputationUpdateEvent(event, ack);

        verify(fcmDispatchService, never()).sendBatch(anyList());
        verify(ack).acknowledge();
    }
}
