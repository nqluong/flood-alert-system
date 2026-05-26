package org.project.floodalert.floodprocessor.messaging.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventProcessorService;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAggregatorListenerTest {

    @Mock
    Acknowledgment acknowledgment;

    @Mock
    FloodEventProcessorService floodEventProcessorService;
    @InjectMocks
    EventAggregatorListener eventAggregatorListener;

    @Test
    void aggregator_nullMessages_acknowledgesAndReturns() {
        eventAggregatorListener.consumeBatch(null, acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(floodEventProcessorService);
    }

    @Test
    void aggregator_emptyMessages_acknowledgesAndReturns() {
        eventAggregatorListener.consumeBatch(Collections.emptyList(), acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(floodEventProcessorService);
    }

    @Test
    void aggregator_success_acknowledges() {
        List<ProcessedSensorData> messages = List.of(new ProcessedSensorData());
        eventAggregatorListener.consumeBatch(messages, acknowledgment);

        verify(floodEventProcessorService).processBatch(messages);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void aggregator_processingThrows_stillAcknowledges() {
        List<ProcessedSensorData> messages = List.of(new ProcessedSensorData());
        doThrow(new RuntimeException("aggregator error")).when(floodEventProcessorService).processBatch(messages);

        eventAggregatorListener.consumeBatch(messages, acknowledgment);

        verify(acknowledgment).acknowledge();
    }



}
