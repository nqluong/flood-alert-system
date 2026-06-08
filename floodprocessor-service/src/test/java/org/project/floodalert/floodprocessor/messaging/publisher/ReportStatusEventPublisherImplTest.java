package org.project.floodalert.floodprocessor.messaging.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.event.ReportStatusUpdateEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportStatusEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ReportStatusEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "reportStatusTopic", "report-status-update");
    }

    @Test
    void publish_nullEvent_skips() {
        publisher.publish(null);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_nullReportId_skips() {
        ReportStatusUpdateEvent event = ReportStatusUpdateEvent.builder().reportId(null).build();
        publisher.publish(event);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_success_sendsWithReportIdAsKey() {
        ReportStatusUpdateEvent event = buildEvent("report-1", "APPROVED");

        SendResult<String, Object> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("report-status-update", 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        publisher.publish(event);

        verify(kafkaTemplate).send("report-status-update", "report-1", event);
    }

    @Test
    void publish_failure_logsErrorWithoutThrowing() {
        ReportStatusUpdateEvent event = buildEvent("report-2", "REJECTED");

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        publisher.publish(event);
    }

    private ReportStatusUpdateEvent buildEvent(String reportId, String status) {
        return ReportStatusUpdateEvent.builder()
                .reportId(reportId)
                .status(status)
                .build();
    }
}
