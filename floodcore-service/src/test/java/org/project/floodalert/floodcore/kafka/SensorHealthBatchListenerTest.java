package org.project.floodalert.floodcore.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.event.SensorHealthSyncEvent;
import org.project.floodalert.floodcore.repository.SensorBatchUpdateRepository;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorHealthBatchListenerTest {

    @Mock
    private SensorBatchUpdateRepository sensorBatchUpdateRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private SensorHealthBatchListener listener;

    private SensorHealthSyncEvent event;

    @BeforeEach
    void setUp() {
        event = new SensorHealthSyncEvent();
    }

    @Test
    void consumeBatch_eventsNull_acknowledgesOnly() {
        listener.consumeBatch(null, acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(sensorBatchUpdateRepository);
    }

    @Test
    void consumeBatch_eventsEmpty_acknowledgesOnly() {
        listener.consumeBatch(List.of(), acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(sensorBatchUpdateRepository);
    }

    @Test
    void consumeBatch_eventsNotEmpty_batchUpdatesAndAcknowledges() {
        List<SensorHealthSyncEvent> events = List.of(event);

        listener.consumeBatch(events, acknowledgment);

        verify(sensorBatchUpdateRepository).batchUpdateSensorHealth(events);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeBatch_batchUpdateThrows_stillAcknowledges() {
        List<SensorHealthSyncEvent> events = List.of(event);

        doThrow(new RuntimeException("DB error"))
                .when(sensorBatchUpdateRepository)
                .batchUpdateSensorHealth(events);

        listener.consumeBatch(events, acknowledgment);

        verify(sensorBatchUpdateRepository).batchUpdateSensorHealth(events);
        verify(acknowledgment).acknowledge();
    }
}