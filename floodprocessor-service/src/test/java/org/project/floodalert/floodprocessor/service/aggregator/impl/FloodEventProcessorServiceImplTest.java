package org.project.floodalert.floodprocessor.service.aggregator.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.messaging.publisher.LifecycleEventPublisher;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService.FloodEventDbResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloodEventProcessorServiceImplTest {
    @Mock
    private FloodEventDbService floodEventDbService;

    @Mock
    private LifecycleEventPublisher lifecycleEventPublisher;

    @InjectMocks
    private FloodEventProcessorServiceImpl service;

    private ProcessedSensorData buildData(String sensorId, FloodStatus status) {
        return ProcessedSensorData.builder()
                .sensorId(sensorId)
                .status(status)
                .waterLevel(150.0)
                .lat(10.75)
                .lon(106.67)
                .locationName("Test Location")
                .build();
    }

    private FloodEvent buildFloodEvent(String sensorId, String severity) {
        return FloodEvent.builder()
                .id(UUID.randomUUID())
                .eventId("EV-20240101-000000-" + sensorId)
                .source("SENSOR")
                .sourceId(sensorId)
                .lat(10.75)
                .lon(106.67)
                .locationDescription("Test Location")
                .waterLevel(BigDecimal.valueOf(150.0))
                .severityLevel(severity)
                .status("CONFIRMED")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    @Test
    void processBatch_nullList_returnsEarlyWithoutCallingDbService() {
        service.processBatch(null);

        verifyNoInteractions(floodEventDbService, lifecycleEventPublisher);
    }

    @Test
    void processBatch_emptyList_returnsEarlyWithoutCallingDbService() {
        service.processBatch(Collections.emptyList());

        verifyNoInteractions(floodEventDbService, lifecycleEventPublisher);
    }

    @Test
    void processBatch_withCreatedEvent_publishesLifecycleEvent() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.WARNING);
        FloodEvent event = buildFloodEvent("SENSOR-001", "WARNING");
        FloodEventDbResult result = new FloodEventDbResult(event, LifecycleEventType.CREATED, true, 150.0, "WARNING");

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(result));

        service.processBatch(List.of(data));

        verify(lifecycleEventPublisher).publishAll(anyList());
    }

    @Test
    void processBatch_withResolvedEvent_publishesLifecycleEvent() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.SAFE);
        FloodEvent event = buildFloodEvent("SENSOR-001", "WARNING");
        FloodEventDbResult result = new FloodEventDbResult(event, LifecycleEventType.RESOLVED, true, 120.0, "WARNING");

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(result));

        service.processBatch(List.of(data));

        verify(lifecycleEventPublisher).publishAll(anyList());
    }

    @Test
    void processBatch_withEscalatedEvent_publishesLifecycleEvent() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.DANGER);
        FloodEvent event = buildFloodEvent("SENSOR-001", "DANGER");
        FloodEventDbResult result = new FloodEventDbResult(event, LifecycleEventType.ESCALATED, true, 200.0, "DANGER");

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(result));

        service.processBatch(List.of(data));

        verify(lifecycleEventPublisher).publishAll(anyList());
    }

    @Test
    void processBatch_withNoActionResult_doesNotPublish() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.WARNING);
        when(floodEventDbService.processAndSaveBatch(anyList()))
                .thenReturn(List.of(FloodEventDbResult.noAction()));

        service.processBatch(List.of(data));

        verifyNoInteractions(lifecycleEventPublisher);
    }

    @Test
    void processBatch_withNoChangeResult_doesNotPublish() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.WARNING);
        FloodEvent event = buildFloodEvent("SENSOR-001", "WARNING");
        // NO_CHANGE: shouldPublish=false, lifecycleEventType=null
        FloodEventDbResult result = new FloodEventDbResult(event, null, false, null, null);

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(result));

        service.processBatch(List.of(data));

        verifyNoInteractions(lifecycleEventPublisher);
    }

    @Test
    void processBatch_floodEventNull_doesNotPublishEvenIfShouldPublishTrue() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.WARNING);
        // Defensive: shouldPublish=true but floodEvent=null should not publish
        FloodEventDbResult result = new FloodEventDbResult(null, LifecycleEventType.CREATED, true, null, null);

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(result));

        service.processBatch(List.of(data));

        verifyNoInteractions(lifecycleEventPublisher);
    }

    @Test
    void processBatch_multipleSensors_publishesForEachShouldPublishResult() {
        ProcessedSensorData d1 = buildData("S-001", FloodStatus.WARNING);
        ProcessedSensorData d2 = buildData("S-002", FloodStatus.DANGER);
        ProcessedSensorData d3 = buildData("S-003", FloodStatus.SAFE);

        FloodEventDbResult r1 = new FloodEventDbResult(
                buildFloodEvent("S-001", "WARNING"), LifecycleEventType.CREATED, true, 150.0, "WARNING");
        FloodEventDbResult r2 = new FloodEventDbResult(
                buildFloodEvent("S-002", "DANGER"), LifecycleEventType.ESCALATED, true, 200.0, "DANGER");
        FloodEventDbResult r3 = FloodEventDbResult.noAction(); // SAFE no active

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(r1, r2, r3));

        service.processBatch(List.of(d1, d2, d3));

        verify(lifecycleEventPublisher).publishAll(argThat(events -> events.size() == 2));
    }

    @Test
    void processBatch_whenProcessAndSaveBatchThrows_doesNotPropagateException() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.WARNING);
        when(floodEventDbService.processAndSaveBatch(anyList()))
                .thenThrow(new RuntimeException("DB connection lost"));

        // Exception is caught and logged inside processBatch
        assertThatCode(() -> service.processBatch(List.of(data))).doesNotThrowAnyException();

        verifyNoInteractions(lifecycleEventPublisher);
    }

    @Test
    void processBatch_whenPublishThrows_doesNotPropagateException() {
        ProcessedSensorData data = buildData("SENSOR-001", FloodStatus.WARNING);
        FloodEvent event = buildFloodEvent("SENSOR-001", "WARNING");
        FloodEventDbResult result = new FloodEventDbResult(event, LifecycleEventType.CREATED, true, 150.0, "WARNING");

        when(floodEventDbService.processAndSaveBatch(anyList())).thenReturn(List.of(result));
        // publishAll() propagates exception to outer catch in processBatch
        doThrow(new RuntimeException("Kafka error")).when(lifecycleEventPublisher).publishAll(anyList());

        assertThatCode(() -> service.processBatch(List.of(data))).doesNotThrowAnyException();
    }
}
