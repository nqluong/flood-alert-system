package org.project.floodalert.floodcore.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.event.UserReportEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserReportEventProducer producer;

    private UserReportEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "userReportTopic", "user-report-events");

        event = new UserReportEvent();
        event.setReportId("report-001");
    }

    @Test
    void publish_sendSuccess_runsCallback() {
        SendResult<String, Object> sendResult = mock(SendResult.class);
        RecordMetadata recordMetadata = mock(RecordMetadata.class);

        when(recordMetadata.topic()).thenReturn("user-report-events");
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(1L);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send("user-report-events", "report-001", event))
                .thenReturn(future);

        producer.publish(event);

        verify(kafkaTemplate).send("user-report-events", "report-001", event);
        verify(sendResult).getRecordMetadata();
        verify(recordMetadata).topic();
        verify(recordMetadata).partition();
        verify(recordMetadata).offset();
    }

    @Test
    void publish_sendFailure_runsCallback() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));

        when(kafkaTemplate.send("user-report-events", "report-001", event))
                .thenReturn(future);

        producer.publish(event);

        verify(kafkaTemplate).send("user-report-events", "report-001", event);
    }
}