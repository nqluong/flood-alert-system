package org.project.floodalert.floodprocessor.service.aggregator.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.repository.IotReadingRepository;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService.FloodEventDbResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloodEventDbServiceImplTest {

    @Mock
    private FloodEventRepository floodEventRepository;

    @Mock
    private IotReadingRepository iotReadingRepository;

    @InjectMocks
    private FloodEventDbServiceImpl service;

    private ProcessedSensorData buildData(FloodStatus status) {
        return ProcessedSensorData.builder()
                .sensorId("SENSOR-001")
                .status(status)
                .waterLevel(150.0)
                .lat(10.75)
                .lon(106.67)
                .locationName("Test Location")
                .build();
    }

    private ProcessedSensorData buildDataWithReadingId(FloodStatus status, String readingId) {
        return ProcessedSensorData.builder()
                .sensorId("SENSOR-001")
                .status(status)
                .waterLevel(150.0)
                .lat(10.75)
                .lon(106.67)
                .locationName("Test Location")
                .readingId(readingId)
                .build();
    }

    private FloodEvent buildActiveEvent(String severityLevel) {
        return FloodEvent.builder()
                .id(UUID.randomUUID())
                .eventId("EV-20240101-000000-SENSOR-001")
                .source("SENSOR")
                .sourceId("SENSOR-001")
                .lat(10.75)
                .lon(106.67)
                .waterLevel(BigDecimal.valueOf(120.0))
                .severityLevel(severityLevel)
                .status("CONFIRMED")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    @Test
    void processAndSave_safe_noActiveEvent_returnsNoAction() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.empty());

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.SAFE));

        assertThat(result.floodEvent()).isNull();
        assertThat(result.shouldPublish()).isFalse();
        assertThat(result.lifecycleEventType()).isNull();
    }

    @Test
    void processAndSave_safe_hasActiveEvent_returnsResolved() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.of(buildActiveEvent("WARNING")));

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.SAFE));

        assertThat(result.lifecycleEventType()).isEqualTo(LifecycleEventType.RESOLVED);
        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.floodEvent().getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void processAndSave_warning_noActiveEvent_createsNewEvent() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.empty());

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.WARNING));

        assertThat(result.lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
        assertThat(result.floodEvent()).isNotNull();
        assertThat(result.floodEvent().getSeverityLevel()).isEqualTo("WARNING");
        assertThat(result.floodEvent().getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.shouldPublish()).isTrue();
    }

    @Test
    void processAndSave_danger_noActiveEvent_createsNewEvent() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.empty());

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.DANGER));

        assertThat(result.lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
        assertThat(result.floodEvent().getSeverityLevel()).isEqualTo("DANGER");
    }

    @Test
    void processAndSave_warning_hasActiveWarning_returnsNoChange() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.of(buildActiveEvent("WARNING")));

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.WARNING));

        assertThat(result.lifecycleEventType()).isNull();
        assertThat(result.shouldPublish()).isFalse();
    }

    @Test
    void processAndSave_danger_hasActiveWarning_returnsEscalated() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.of(buildActiveEvent("WARNING")));

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.DANGER));

        assertThat(result.lifecycleEventType()).isEqualTo(LifecycleEventType.ESCALATED);
        assertThat(result.shouldPublish()).isTrue();
    }

    @Test
    void processAndSave_warning_hasActiveDanger_returnsDeEscalated() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.of(buildActiveEvent("DANGER")));

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.WARNING));

        assertThat(result.lifecycleEventType()).isEqualTo(LifecycleEventType.DE_ESCALATED);
        assertThat(result.shouldPublish()).isTrue();
    }

    @Test
    void processAndSave_scenarioC_nullWaterLevelFromSensor_keepsExistingWaterLevel() {
        FloodEvent activeEvent = buildActiveEvent("WARNING");
        BigDecimal originalLevel = activeEvent.getWaterLevel();

        ProcessedSensorData data = ProcessedSensorData.builder()
                .sensorId("SENSOR-001").status(FloodStatus.WARNING)
                .waterLevel(null).lat(10.75).lon(106.67)
                .locationName("Test Location").build();

        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.of(activeEvent));

        service.processAndSave(data);

        assertThat(activeEvent.getWaterLevel()).isEqualTo(originalLevel);
    }

    @Test
    void processAndSave_unknownStatus_returnsNoAction() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.empty());

        FloodEventDbResult result = service.processAndSave(buildData(FloodStatus.UNKNOWN));

        assertThat(result.floodEvent()).isNull();
        assertThat(result.shouldPublish()).isFalse();
    }

    @Test
    void processAndSave_withReadingId_callsBackLink() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.empty());

        service.processAndSave(buildDataWithReadingId(FloodStatus.WARNING, "READING-001"));

        verify(iotReadingRepository).updateFloodEventIdByReadingId(isNull(), eq("READING-001"));
    }

    @Test
    void processAndSave_backLinkThrowsException_doesNotPropagateError() {
        when(floodEventRepository.findActiveEventBySensorId(anyString(), any()))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB error"))
                .when(iotReadingRepository).updateFloodEventIdByReadingId(any(), anyString());

        // try-catch inside backLinkIotReading swallows the error
        FloodEventDbResult result =
                service.processAndSave(buildDataWithReadingId(FloodStatus.WARNING, "READING-001"));

        assertThat(result).isNotNull();
    }

    @Test
    void processAndSaveBatch_nullList_returnsEmptyList() {
        assertThat(service.processAndSaveBatch(null)).isEmpty();
        verifyNoInteractions(floodEventRepository, iotReadingRepository);
    }

    @Test
    void processAndSaveBatch_emptyList_returnsEmptyList() {
        assertThat(service.processAndSaveBatch(Collections.emptyList())).isEmpty();
        verifyNoInteractions(floodEventRepository, iotReadingRepository);
    }

    @Test
    void processAndSaveBatch_safe_noActiveEvent_returnsNoAction() {
        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(buildData(FloodStatus.SAFE)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).floodEvent()).isNull();
        verify(floodEventRepository, never()).saveAll(any());
    }

    @Test
    void processAndSaveBatch_safe_hasActiveEvent_returnsResolved() {
        FloodEvent activeEvent = buildActiveEvent("WARNING");
        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(List.of(activeEvent));
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(buildData(FloodStatus.SAFE)));

        assertThat(results.get(0).lifecycleEventType()).isEqualTo(LifecycleEventType.RESOLVED);
        verify(floodEventRepository).saveAll(any());
    }

    @Test
    void processAndSaveBatch_warning_noActiveEvent_createsAndSavesEvent() {
        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(buildData(FloodStatus.WARNING)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
        verify(floodEventRepository).saveAll(any());
    }

    @Test
    void processAndSaveBatch_multipleSensors_processesAll() {
        ProcessedSensorData d1 = ProcessedSensorData.builder()
                .sensorId("S-001").status(FloodStatus.WARNING).waterLevel(150.0)
                .lat(10.0).lon(106.0).locationName("Loc1").build();
        ProcessedSensorData d2 = ProcessedSensorData.builder()
                .sensorId("S-002").status(FloodStatus.DANGER).waterLevel(200.0)
                .lat(10.1).lon(106.1).locationName("Loc2").build();

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results = service.processAndSaveBatch(List.of(d1, d2));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
        assertThat(results.get(1).lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
    }


    @Test
    void processAndSaveBatch_sameSensor_resolvedThenWarning_tracksStateCorrectly() {
        FloodEvent existing = buildActiveEvent("WARNING");
        ProcessedSensorData safeData = buildData(FloodStatus.SAFE);
        ProcessedSensorData warnData = buildData(FloodStatus.WARNING);

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(List.of(existing));
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(safeData, warnData));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).lifecycleEventType()).isEqualTo(LifecycleEventType.RESOLVED);
        assertThat(results.get(1).lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
    }

    @Test
    void processAndSaveBatch_sameSensor_createdThenWarning_tracksStateCorrectly() {
        // Message 1: WARNING → creates new event (CREATED)
        // Message 2: WARNING for same sensor → sees the new event in map → NO_CHANGE
        ProcessedSensorData warn1 = buildData(FloodStatus.WARNING);
        ProcessedSensorData warn2 = buildData(FloodStatus.WARNING);

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(warn1, warn2));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).lifecycleEventType()).isEqualTo(LifecycleEventType.CREATED);
        assertThat(results.get(1).lifecycleEventType()).isNull(); // NO_CHANGE
    }

    @Test
    void processAndSaveBatch_withReadingId_findAllByReadingIdsCalled() {
        ProcessedSensorData data = buildDataWithReadingId(FloodStatus.WARNING, "READING-001");

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        IoTReading reading = IoTReading.builder()
                .id(UUID.randomUUID()).readingId("READING-001").sensorId("SENSOR-001").build();
        when(iotReadingRepository.findAllByReadingIds(anyList())).thenReturn(List.of(reading));
        when(iotReadingRepository.saveAll(any())).thenReturn(Collections.emptyList());

        service.processAndSaveBatch(List.of(data));

        verify(iotReadingRepository).findAllByReadingIds(List.of("READING-001"));
        verify(iotReadingRepository).saveAll(any());
    }

    @Test
    void processAndSaveBatch_readingNotFound_iotSaveAllNotCalled() {
        ProcessedSensorData data = buildDataWithReadingId(FloodStatus.WARNING, "READING-MISSING");

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());
        when(iotReadingRepository.findAllByReadingIds(anyList()))
                .thenReturn(Collections.emptyList());

        service.processAndSaveBatch(List.of(data));

        verify(iotReadingRepository, never()).saveAll(any());
    }

    @Test
    void processAndSaveBatch_batchBackLinkThrows_fallsBackToIndividualUpdate() {
        ProcessedSensorData data = buildDataWithReadingId(FloodStatus.WARNING, "READING-001");

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());
        when(iotReadingRepository.findAllByReadingIds(anyList()))
                .thenThrow(new RuntimeException("DB error"));

        service.processAndSaveBatch(List.of(data));

        verify(iotReadingRepository).updateFloodEventIdByReadingId(isNull(), eq("READING-001"));
    }

    @Test
    void processAndSaveBatch_batchBackLinkFallback_fallbackThrows_continuesGracefully() {
        ProcessedSensorData data = buildDataWithReadingId(FloodStatus.WARNING, "READING-001");

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());
        when(iotReadingRepository.findAllByReadingIds(anyList()))
                .thenThrow(new RuntimeException("batch error"));
        doThrow(new RuntimeException("individual error"))
                .when(iotReadingRepository).updateFloodEventIdByReadingId(any(), anyString());

        // Should not propagate — each individual failure is caught and logged
        List<FloodEventDbResult> results = service.processAndSaveBatch(List.of(data));

        assertThat(results).hasSize(1);
    }

    @Test
    void processAndSaveBatch_scenarioC_nullPreviousSeverity_treatedAsNoChange() {
        FloodEvent activeEvent = FloodEvent.builder()
                .id(UUID.randomUUID()).eventId("EV-001")
                .source("SENSOR").sourceId("SENSOR-001")
                .lat(10.75).lon(106.67)
                .waterLevel(BigDecimal.valueOf(120.0))
                .severityLevel(null)   // null → NO_CHANGE
                .status("CONFIRMED")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(List.of(activeEvent));
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(buildData(FloodStatus.WARNING)));

        assertThat(results.get(0).lifecycleEventType()).isNull();
        assertThat(results.get(0).shouldPublish()).isFalse();
    }

    @Test
    void processAndSaveBatch_scenarioC_invalidPreviousSeverityEnum_treatedAsNoChange() {
        FloodEvent activeEvent = FloodEvent.builder()
                .id(UUID.randomUUID()).eventId("EV-001")
                .source("SENSOR").sourceId("SENSOR-001")
                .lat(10.75).lon(106.67)
                .waterLevel(BigDecimal.valueOf(120.0))
                .severityLevel("INVALID_ENUM")
                .status("CONFIRMED")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(List.of(activeEvent));
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(buildData(FloodStatus.WARNING)));

        assertThat(results.get(0).lifecycleEventType()).isNull();
    }

    @Test
    void processAndSaveBatch_scenarioC_nullWaterLevelFromSensor_keepsExistingLevel() {
        FloodEvent activeEvent = buildActiveEvent("WARNING");
        BigDecimal originalLevel = activeEvent.getWaterLevel();

        ProcessedSensorData data = ProcessedSensorData.builder()
                .sensorId("SENSOR-001").status(FloodStatus.WARNING)
                .waterLevel(null)
                .lat(10.75).lon(106.67).locationName("Test Location").build();

        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(List.of(activeEvent));
        when(floodEventRepository.saveAll(any())).thenReturn(Collections.emptyList());

        service.processAndSaveBatch(List.of(data));

        assertThat(activeEvent.getWaterLevel()).isEqualTo(originalLevel);
    }

    @Test
    void processAndSaveBatch_unknownStatus_returnsNoAction() {
        when(floodEventRepository.findActiveEventsBySensorIds(anyList(), any()))
                .thenReturn(Collections.emptyList());

        List<FloodEventDbResult> results =
                service.processAndSaveBatch(List.of(buildData(FloodStatus.UNKNOWN)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).floodEvent()).isNull();
        verify(floodEventRepository, never()).saveAll(any());
    }
}
