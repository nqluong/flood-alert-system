package org.project.floodalert.floodprocessor.service.assessment.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.mapper.SensorDataMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloodAssessmentServiceImplTest {

    @Mock
    private FloodStatusCalculator floodStatusCalculator;

    @Mock
    private SensorDataMapper sensorDataMapper;

    @InjectMocks
    private FloodAssessmentServiceImpl service;

    // ==================== assessFloodStatus(List) ====================

    @Test
    void assessList_nullInput_throwsAppException() {
        assertThrows(AppException.class, () -> service.assessFloodStatus((List<EnrichedSensorData>) null));
    }

    @Test
    void assessList_emptyInput_returnsEmptyList() {
        List<ProcessedSensorData> result = service.assessFloodStatus(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    // logProcessingStatistics: tất cả SAFE, không có UNKNOWN/DANGER
    @Test
    void assessList_allSafe_logsStatistics() {
        EnrichedSensorData enriched = mockEnriched();
        ProcessedSensorData processed = mockProcessed(FloodStatus.SAFE);

        when(floodStatusCalculator.calculate(any(), any(), any())).thenReturn(FloodStatus.SAFE);
        when(sensorDataMapper.mapToProcessedData(eq(enriched), eq(FloodStatus.SAFE))).thenReturn(processed);

        service.assessFloodStatus(List.of(enriched));

        verify(sensorDataMapper).mapToProcessedData(enriched, FloodStatus.SAFE);
    }

    // logProcessingStatistics: có UNKNOWN → log warn
    @Test
    void assessList_hasUnknown_logsUnknownWarning() {
        EnrichedSensorData enriched = mockEnriched();
        ProcessedSensorData processed = mockProcessed(FloodStatus.UNKNOWN);

        when(floodStatusCalculator.calculate(any(), any(), any())).thenReturn(FloodStatus.UNKNOWN);
        when(sensorDataMapper.mapToProcessedData(eq(enriched), eq(FloodStatus.UNKNOWN))).thenReturn(processed);

        service.assessFloodStatus(List.of(enriched));

        verify(sensorDataMapper).mapToProcessedData(enriched, FloodStatus.UNKNOWN);
    }

    // logProcessingStatistics: có DANGER → log warn
    @Test
    void assessList_hasDanger_logsDangerWarning() {
        EnrichedSensorData enriched = mockEnriched();
        ProcessedSensorData processed = mockProcessed(FloodStatus.DANGER);

        when(floodStatusCalculator.calculate(any(), any(), any())).thenReturn(FloodStatus.DANGER);
        when(sensorDataMapper.mapToProcessedData(eq(enriched), eq(FloodStatus.DANGER))).thenReturn(processed);

        service.assessFloodStatus(List.of(enriched));

        verify(sensorDataMapper).mapToProcessedData(enriched, FloodStatus.DANGER);
    }

    // logProcessingStatistics: WARNING được đếm
    @Test
    void assessList_hasWarning_logsStatistics() {
        EnrichedSensorData enriched = mockEnriched();
        ProcessedSensorData processed = mockProcessed(FloodStatus.WARNING);

        when(floodStatusCalculator.calculate(any(), any(), any())).thenReturn(FloodStatus.WARNING);
        when(sensorDataMapper.mapToProcessedData(eq(enriched), eq(FloodStatus.WARNING))).thenReturn(processed);

        service.assessFloodStatus(List.of(enriched));

        verify(sensorDataMapper).mapToProcessedData(enriched, FloodStatus.WARNING);
    }


    @Test
    void assessSingle_nullInput_throwsAppException() {
        assertThrows(AppException.class, () -> service.assessFloodStatus((EnrichedSensorData) null));
    }


    private EnrichedSensorData mockEnriched() {
        EnrichedSensorData data = mock(EnrichedSensorData.class);
        when(data.getSensorId()).thenReturn("s1");
        when(data.getWaterLevel()).thenReturn(1.0);
        when(data.getWarningThreshold()).thenReturn(3.0);
        when(data.getDangerThreshold()).thenReturn(5.0);
        return data;
    }

    private ProcessedSensorData mockProcessed(FloodStatus status) {
        ProcessedSensorData data = mock(ProcessedSensorData.class);
        when(data.getStatus()).thenReturn(status);
        return data;
    }
}