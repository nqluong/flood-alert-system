package org.project.floodalert.floodprocessor.messaging.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LifecycleEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LifecycleEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "lifecycleTopic", "flood-lifecycle-events");
    }

    @Test
    void publish_nullEvent_skips() {
        publisher.publish(null);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_nullEventId_skips() {
        FloodLifecycleEvent event = FloodLifecycleEvent.builder().eventId(null).build();
        publisher.publish(event);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_success_logsMetadata() {
        FloodLifecycleEvent event = buildEvent("evt-1");

        SendResult<String, Object> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("flood-lifecycle-events", 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        publisher.publish(event);
    }

    @Test
    void publish_failure_logsError() {
        FloodLifecycleEvent event = buildEvent("evt-2");

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        publisher.publish(event);
    }

    private FloodLifecycleEvent buildEvent(String eventId) {
        return FloodLifecycleEvent.builder().eventId(eventId).build();
    }
}