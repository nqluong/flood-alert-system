package org.project.floodalert.floodcore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.request.*;
import org.project.floodalert.floodcore.dto.response.*;
import org.project.floodalert.floodcore.service.SensorService;
import org.project.floodalert.floodcore.service.SensorUpdateService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorControllerTest {

    @Mock
    private SensorService sensorService;

    @Mock
    private SensorUpdateService sensorUpdateService;

    @InjectMocks
    private SensorController sensorController;

    private UUID userId;
    private UUID sensorUuid;
    private String sensorId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sensorUuid = UUID.randomUUID();
        sensorId = "SENS-HAN-01";
    }

    @Test
    void createSensor_callsService() {
        CreateSensorRequest request = mock(CreateSensorRequest.class);
        CreateSensorResponse response = mock(CreateSensorResponse.class);

        when(request.getSensorId()).thenReturn(sensorId);
        when(response.getSensorId()).thenReturn(sensorId);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorService.createSensor(request, userId))
                    .thenReturn(response);

            sensorController.createSensor(request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorService).createSensor(request, userId);
        }
    }

    @Test
    void batchCreateSensors_callsService() {
        BatchCreateSensorRequest request = mock(BatchCreateSensorRequest.class);
        BatchCreateSensorResponse response = mock(BatchCreateSensorResponse.class);

        when(request.getSensors()).thenReturn(List.of(mock(CreateSensorRequest.class)));
        when(response.getSuccessCount()).thenReturn(1);
        when(response.getFailureCount()).thenReturn(0);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorService.batchCreateSensors(request, userId))
                    .thenReturn(response);

            sensorController.batchCreateSensors(request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorService).batchCreateSensors(request, userId);
        }
    }

    @Test
    void getSensorList_callsService() {
        SensorFilterRequest filter = mock(SensorFilterRequest.class);
        PageResponse<SensorSummaryResponse> response = mock(PageResponse.class);

        when(response.getContent()).thenReturn(List.of(mock(SensorSummaryResponse.class)));
        when(sensorService.getSensorList(filter)).thenReturn(response);

        sensorController.getSensorList(filter);

        verify(sensorService).getSensorList(filter);
    }

    @Test
    void getSensorDetail_callsService() {
        SensorDetailResponse response = mock(SensorDetailResponse.class);

        when(sensorService.getSensorDetail(sensorUuid, true))
                .thenReturn(response);

        sensorController.getSensorDetail(sensorUuid, true);

        verify(sensorService).getSensorDetail(sensorUuid, true);
    }

    @Test
    void getSensorDetailBySensorId_callsService() {
        SensorDetailResponse response = mock(SensorDetailResponse.class);

        when(sensorService.getSensorDetailBySensorId(sensorId, false))
                .thenReturn(response);

        sensorController.getSensorDetailBySensorId(sensorId, false);

        verify(sensorService).getSensorDetailBySensorId(sensorId, false);
    }

    @Test
    void getSensorMapData_callsService() {
        SensorMapResponse response = mock(SensorMapResponse.class);

        when(sensorService.getSensorMapData()).thenReturn(response);

        sensorController.getSensorMapData();

        verify(sensorService).getSensorMapData();
    }

    @Test
    void getActiveSensorMapData_callsService() {
        SensorMapResponse response = mock(SensorMapResponse.class);

        when(sensorService.getActiveSensorMapData()).thenReturn(response);

        sensorController.getActiveSensorMapData();

        verify(sensorService).getActiveSensorMapData();
    }

    @Test
    void updateSensor_callsService() {
        UpdateSensorRequest request = mock(UpdateSensorRequest.class);
        UpdateSensorResponse response = mock(UpdateSensorResponse.class);

        when(response.getChangedFields()).thenReturn(List.of("location"));

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.updateSensor(sensorUuid, request, userId))
                    .thenReturn(response);

            sensorController.updateSensor(sensorUuid, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).updateSensor(sensorUuid, request, userId);
        }
    }

    @Test
    void updateSensorBySensorId_callsService() {
        UpdateSensorRequest request = mock(UpdateSensorRequest.class);
        UpdateSensorResponse response = mock(UpdateSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.updateSensorBySensorId(sensorId, request, userId))
                    .thenReturn(response);

            sensorController.updateSensorBySensorId(sensorId, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).updateSensorBySensorId(sensorId, request, userId);
        }
    }

    @Test
    void softDeleteSensor_callsService() {
        DeleteSensorRequest request = mock(DeleteSensorRequest.class);
        DeleteSensorResponse response = mock(DeleteSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.softDelete(sensorUuid, request, userId))
                    .thenReturn(response);

            sensorController.softDeleteSensor(sensorUuid, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).softDelete(sensorUuid, request, userId);
        }
    }

    @Test
    void softDeleteBySensorId_callsService() {
        DeleteSensorRequest request = mock(DeleteSensorRequest.class);
        DeleteSensorResponse response = mock(DeleteSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.softDeleteBySensorId(sensorId, request, userId))
                    .thenReturn(response);

            sensorController.softDeleteBySensorId(sensorId, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).softDeleteBySensorId(sensorId, request, userId);
        }
    }

    @Test
    void hardDeleteSensor_callsService() {
        DeleteSensorRequest request = mock(DeleteSensorRequest.class);
        DeleteSensorResponse response = mock(DeleteSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.hardDelete(sensorUuid, request, userId))
                    .thenReturn(response);

            sensorController.hardDeleteSensor(sensorUuid, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).hardDelete(sensorUuid, request, userId);
        }
    }

    @Test
    void hardDeleteBySensorId_callsService() {
        DeleteSensorRequest request = mock(DeleteSensorRequest.class);
        DeleteSensorResponse response = mock(DeleteSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.hardDeleteBySensorId(sensorId, request, userId))
                    .thenReturn(response);

            sensorController.hardDeleteBySensorId(sensorId, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).hardDeleteBySensorId(sensorId, request, userId);
        }
    }

    @Test
    void restoreSensor_callsService() {
        DeleteSensorResponse response = mock(DeleteSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.restoreSensor(sensorUuid, userId))
                    .thenReturn(response);

            sensorController.restoreSensor(sensorUuid);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).restoreSensor(sensorUuid, userId);
        }
    }

    @Test
    void restoreBySensorId_callsService() {
        DeleteSensorResponse response = mock(DeleteSensorResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.restoreSensorBySensorId(sensorId, userId))
                    .thenReturn(response);

            sensorController.restoreBySensorId(sensorId);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).restoreSensorBySensorId(sensorId, userId);
        }
    }

    @Test
    void changeStatus_callsService() {
        ChangeStatusRequest request = mock(ChangeStatusRequest.class);
        ChangeStatusResponse response = mock(ChangeStatusResponse.class);

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.changeStatus(sensorUuid, request, userId))
                    .thenReturn(response);

            sensorController.changeStatus(sensorUuid, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).changeStatus(sensorUuid, request, userId);
        }
    }

    @Test
    void changeStatusBySensorId_callsService() {
        ChangeStatusRequest request = mock(ChangeStatusRequest.class);
        ChangeStatusResponse response = mock(ChangeStatusResponse.class);

        when(request.getNewStatus()).thenReturn("MAINTENANCE");
        when(response.getPreviousStatus()).thenReturn("ACTIVE");
        when(response.getCurrentStatus()).thenReturn("MAINTENANCE");

        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(sensorUpdateService.changeStatusBySensorId(sensorId, request, userId))
                    .thenReturn(response);

            sensorController.changeStatusBySensorId(sensorId, request);

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(sensorUpdateService).changeStatusBySensorId(sensorId, request, userId);
        }
    }
}