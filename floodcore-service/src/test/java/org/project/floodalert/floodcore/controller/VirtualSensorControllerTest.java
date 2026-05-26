package org.project.floodalert.floodcore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.request.VirtualSensorProvisionRequest;
import org.project.floodalert.floodcore.dto.response.VirtualSensorCleanupResponse;
import org.project.floodalert.floodcore.dto.response.VirtualSensorProvisionResponse;
import org.project.floodalert.floodcore.service.VirtualSensorService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirtualSensorControllerTest {

    @Mock
    private VirtualSensorService virtualSensorService;

    @InjectMocks
    private VirtualSensorController virtualSensorController;

    private VirtualSensorProvisionRequest request;

    @BeforeEach
    void setUp() {
        request = mock(VirtualSensorProvisionRequest.class);
    }

    @Test
    void provisionVirtualSensors_callsService() {
        VirtualSensorProvisionResponse response = mock(VirtualSensorProvisionResponse.class);

        when(request.getTargetCount()).thenReturn(10);
        when(virtualSensorService.provisionVirtualSensors(request))
                .thenReturn(response);

        virtualSensorController.provisionVirtualSensors(request);

        verify(request).getTargetCount();
        verify(virtualSensorService).provisionVirtualSensors(request);
    }

    @Test
    void cleanupVirtualSensors_callsService() {
        VirtualSensorCleanupResponse response = mock(VirtualSensorCleanupResponse.class);

        when(virtualSensorService.cleanupVirtualSensors())
                .thenReturn(response);

        virtualSensorController.cleanupVirtualSensors();

        verify(virtualSensorService).cleanupVirtualSensors();
    }

    @Test
    void provisionVirtualSensors_serviceThrows_stillCallsService() {
        when(request.getTargetCount()).thenReturn(10);
        when(virtualSensorService.provisionVirtualSensors(request))
                .thenThrow(new RuntimeException("Provision error"));

        try {
            virtualSensorController.provisionVirtualSensors(request);
        } catch (RuntimeException ignored) {
        }

        verify(request).getTargetCount();
        verify(virtualSensorService).provisionVirtualSensors(request);
    }

    @Test
    void cleanupVirtualSensors_serviceThrows_stillCallsService() {
        when(virtualSensorService.cleanupVirtualSensors())
                .thenThrow(new RuntimeException("Cleanup error"));

        try {
            virtualSensorController.cleanupVirtualSensors();
        } catch (RuntimeException ignored) {
        }

        verify(virtualSensorService).cleanupVirtualSensors();
    }
}