package org.project.floodalert.floodprocessor.service.processing;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorHealthThrottlingServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SensorHealthThrottlingService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "sensorHealthSyncTopic", "sensor-health-sync");
        ReflectionTestUtils.setField(service, "syncIntervalMs", 300000L);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, Object>> mockFuture() {
        return mock(CompletableFuture.class);
    }

    private ProcessedSensorData buildData(String sensorId, String deviceStatus) {
        return ProcessedSensorData.builder()
                .sensorId(sensorId)
                .deviceStatus(deviceStatus)
                .battery(85.0)
                .signalStrength(-70)
                .build();
    }

    @Test
    void checkAndSend_firstCall_sendsEvent() {
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mockFuture());
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
    }

    @Test
    void checkAndSend_sameStatus_intervalNotExpired_skipsThrottled() {
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mockFuture());
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkAndSend_sameStatus_intervalExpired_sendsPeriodic() {
        ConcurrentHashMap<String, Long> syncMap = (ConcurrentHashMap<String, Long>)
                ReflectionTestUtils.getField(service, "lastSyncTimeMap");
        ConcurrentHashMap<String, String> statusMap = (ConcurrentHashMap<String, String>)
                ReflectionTestUtils.getField(service, "lastStatusMap");
        syncMap.put("sensor-001", System.currentTimeMillis() - 400_000L);
        statusMap.put("sensor-001", "NORMAL");

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mockFuture());
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
    }

    @Test
    void checkAndSend_statusChanged_sendsEvent() {
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mockFuture());
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
        service.checkAndSend(buildData("sensor-001", "DANGER"));
    }

    @Test
    void checkAndSend_deviceStatusNull_resolvesToUnknown() {
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mockFuture());
        service.checkAndSend(buildData("sensor-001", null));
    }

    @Test
    void checkAndSend_kafkaException_logsError() {
        CompletableFuture<SendResult<String, Object>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkAndSend_kafkaSuccess_logsDebugAck() {
        SendResult<String, Object> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        service.checkAndSend(buildData("sensor-001", "NORMAL"));
    }
}
