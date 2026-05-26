package org.project.floodalert.floodprocessor.service.core.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.service.assessment.FloodAssessmentService;
import org.project.floodalert.floodprocessor.service.core.DispatcherService;
import org.project.floodalert.floodprocessor.service.state.StateChangeService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessLogicServiceImplTest {

    @Mock private FloodAssessmentService floodAssessmentService;
    @Mock private StateChangeService stateChangeService;
    @Mock private DispatcherService dispatcherService;

    @InjectMocks
    private BusinessLogicServiceImpl service;

    // null → AppException
    @Test
    void process_nullInput_throwsAppException() {
        assertThrows(AppException.class, () -> service.process(null));
        verifyNoInteractions(floodAssessmentService, stateChangeService, dispatcherService);
    }

    // empty → return early
    @Test
    void process_emptyInput_returnsEarly() {
        service.process(Collections.emptyList());
        verifyNoInteractions(floodAssessmentService, stateChangeService, dispatcherService);
    }

    // happy path: assess → detectStateChanges → dispatch
    @Test
    void process_success_callsAllSteps() {
        List<EnrichedSensorData> enriched = List.of(mock(EnrichedSensorData.class));
        List<ProcessedSensorData> processed = List.of(mock(ProcessedSensorData.class));

        when(floodAssessmentService.assessFloodStatus(enriched)).thenReturn(processed);

        service.process(enriched);

        verify(floodAssessmentService).assessFloodStatus(enriched);
        verify(stateChangeService).detectStateChanges(processed);
        verify(dispatcherService).dispatch(processed);
    }

    // assessFloodStatus throws → catch → wrap AppException
    @Test
    void process_assessThrows_wrapsInAppException() {
        List<EnrichedSensorData> enriched = List.of(mock(EnrichedSensorData.class));
        when(floodAssessmentService.assessFloodStatus(anyList()))
                .thenThrow(new RuntimeException("assess error"));

        assertThrows(AppException.class, () -> service.process(enriched));
        verifyNoInteractions(stateChangeService, dispatcherService);
    }

    // stateChangeService throws → catch → wrap AppException
    @Test
    void process_stateChangeThrows_wrapsInAppException() {
        List<EnrichedSensorData> enriched = List.of(mock(EnrichedSensorData.class));
        List<ProcessedSensorData> processed = List.of(mock(ProcessedSensorData.class));

        when(floodAssessmentService.assessFloodStatus(enriched)).thenReturn(processed);
        doThrow(new RuntimeException("state error")).when(stateChangeService).detectStateChanges(processed);

        assertThrows(AppException.class, () -> service.process(enriched));
        verifyNoInteractions(dispatcherService);
    }

    // dispatcherService throws → catch → wrap AppException
    @Test
    void process_dispatchThrows_wrapsInAppException() {
        List<EnrichedSensorData> enriched = List.of(mock(EnrichedSensorData.class));
        List<ProcessedSensorData> processed = List.of(mock(ProcessedSensorData.class));

        when(floodAssessmentService.assessFloodStatus(enriched)).thenReturn(processed);
        doThrow(new RuntimeException("dispatch error")).when(dispatcherService).dispatch(processed);

        assertThrows(AppException.class, () -> service.process(enriched));
    }
}