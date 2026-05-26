package org.project.floodalert.floodprocessor.service.state.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateChangeDetectorTest {

    @InjectMocks
    private StateChangeDetector detector;

    // previousStatus = null → hasChanged = true → logStateChange "lần đầu tiên"
    @Test
    void detectAndUpdate_nullPreviousStatus_marksChangedAndLogsInitial() {
        ProcessedSensorData data = mockData("s1", FloodStatus.SAFE);

        detector.detectAndUpdate(data, null);

        verify(data).setPreviousStatus(null);
        verify(data).setStateChanged(true);
    }

    // currentStatus == previousStatus → hasChanged = false → log trace
    @Test
    void detectAndUpdate_sameStatus_marksNotChanged() {
        ProcessedSensorData data = mockData("s1", FloodStatus.SAFE);

        detector.detectAndUpdate(data, FloodStatus.SAFE);

        verify(data).setStateChanged(false);
    }

    // currentStatus != previousStatus, currentStatus = DANGER → log warn
    @Test
    void detectAndUpdate_changedToDanger_logsWarning() {
        ProcessedSensorData data = mockData("s1", FloodStatus.DANGER);

        detector.detectAndUpdate(data, FloodStatus.SAFE);

        verify(data).setStateChanged(true);
    }

    // previousStatus = DANGER, currentStatus != DANGER → log "thoát NGUY HIỂM"
    @Test
    void detectAndUpdate_exitedDanger_logsRecovery() {
        ProcessedSensorData data = mockData("s1", FloodStatus.SAFE);

        detector.detectAndUpdate(data, FloodStatus.DANGER);

        verify(data).setStateChanged(true);
    }

    // previousStatus != null, currentStatus != previousStatus, không liên quan DANGER
    @Test
    void detectAndUpdate_normalStateChange_marksChanged() {
        ProcessedSensorData data = mockData("s1", FloodStatus.WARNING);

        detector.detectAndUpdate(data, FloodStatus.SAFE);

        verify(data).setStateChanged(true);
    }

    private ProcessedSensorData mockData(String sensorId, FloodStatus status) {
        ProcessedSensorData data = mock(ProcessedSensorData.class);
        when(data.getSensorId()).thenReturn(sensorId);
        when(data.getStatus()).thenReturn(status);
        return data;
    }
}