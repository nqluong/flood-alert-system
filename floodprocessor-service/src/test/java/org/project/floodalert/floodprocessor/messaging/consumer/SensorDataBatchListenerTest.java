package org.project.floodalert.floodprocessor.messaging.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;
import org.project.floodalert.floodprocessor.service.processing.SensorDataBatchProcessor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorDataBatchListenerTest {
    @Mock
    SensorDataBatchProcessor sensorDataBatchProcessor;
    @InjectMocks
    SensorDataBatchListener sensorDataBatchListener;
    @Mock
    Acknowledgment acknowledgment;

    @Test
    void sensorBatch_success_acknowledges() {
        List<SensorMessage> messages = List.of(new SensorMessage());
        sensorDataBatchListener.consumeBatch(messages, acknowledgment);

        verify(sensorDataBatchProcessor).processBatch(messages);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void sensorBatch_processingThrows_stillAcknowledges() {
        List<SensorMessage> messages = List.of(new SensorMessage());
        doThrow(new RuntimeException("batch error")).when(sensorDataBatchProcessor).processBatch(messages);

        sensorDataBatchListener.consumeBatch(messages, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
