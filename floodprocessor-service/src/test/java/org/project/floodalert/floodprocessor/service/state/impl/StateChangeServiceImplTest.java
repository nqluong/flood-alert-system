package org.project.floodalert.floodprocessor.service.state.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.repository.RedisStateRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateChangeServiceImplTest {

    @Mock
    private RedisStateRepository redisStateRepository;

    @Mock
    private StateChangeDetector stateChangeDetector;

    @InjectMocks
    private StateChangeServiceImpl service;

    // null input → IllegalArgumentException
    @Test
    void detectStateChanges_nullInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.detectStateChanges(null));
        verifyNoInteractions(redisStateRepository, stateChangeDetector);
    }

    // empty input → return sớm, không gọi Redis
    @Test
    void detectStateChanges_emptyList_returnsEarly() {
        service.detectStateChanges(Collections.emptyList());
        verifyNoInteractions(redisStateRepository, stateChangeDetector);
    }

    // happy path: previousStatus có sẵn trong Redis
    @Test
    void detectStateChanges_withPreviousStatus_detectsAndSaves() {
        ProcessedSensorData data = mockData("s1", FloodStatus.WARNING);
        when(redisStateRepository.batchGetPreviousStatuses(anySet()))
                .thenReturn(Map.of("s1", FloodStatus.SAFE));

        service.detectStateChanges(List.of(data));

        verify(stateChangeDetector).detectAndUpdate(data, FloodStatus.SAFE);
        verify(redisStateRepository).batchSaveNewStatuses(any());
    }

    // previousStatus không có trong Redis → truyền null vào detector
    @Test
    void detectStateChanges_noPreviousStatus_passesNullToDetector() {
        ProcessedSensorData data = mockData("s2", FloodStatus.SAFE);
        when(redisStateRepository.batchGetPreviousStatuses(anySet()))
                .thenReturn(Collections.emptyMap());

        service.detectStateChanges(List.of(data));

        verify(stateChangeDetector).detectAndUpdate(data, null);
        verify(redisStateRepository).batchSaveNewStatuses(any());
    }

    // multiple sensors trong một batch
    @Test
    void detectStateChanges_multipleSensors_processesAll() {
        ProcessedSensorData d1 = mockData("s1", FloodStatus.SAFE);
        ProcessedSensorData d2 = mockData("s2", FloodStatus.DANGER);
        when(redisStateRepository.batchGetPreviousStatuses(anySet()))
                .thenReturn(Map.of("s1", FloodStatus.WARNING, "s2", FloodStatus.SAFE));

        service.detectStateChanges(List.of(d1, d2));

        verify(stateChangeDetector).detectAndUpdate(d1, FloodStatus.WARNING);
        verify(stateChangeDetector).detectAndUpdate(d2, FloodStatus.SAFE);
        verify(redisStateRepository).batchSaveNewStatuses(any());
    }

    private ProcessedSensorData mockData(String sensorId, FloodStatus status) {
        ProcessedSensorData data = mock(ProcessedSensorData.class);
        when(data.getSensorId()).thenReturn(sensorId);
        when(data.getStatus()).thenReturn(status);
        return data;
    }
}