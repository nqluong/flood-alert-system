package org.project.floodalert.floodprocessor.messaging.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaDispatcherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaDispatcher kafkaDispatcher;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaDispatcher, "outputTopic", "test-output-topic");
    }

    @Test
    void send_success_logsMetadata() {
        ProcessedSensorData data = mockSensorData("sensor-1");

        SendResult<String, Object> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("test-output-topic", 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        kafkaDispatcher.send(data);
    }

    @Test
    void send_failure_logsError() {
        ProcessedSensorData data = mockSensorData("sensor-2");

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        kafkaDispatcher.send(data);
    }

    private ProcessedSensorData mockSensorData(String sensorId) {
        ProcessedSensorData data = mock(ProcessedSensorData.class);
        when(data.getSensorId()).thenReturn(sensorId);
        return data;
    }
}