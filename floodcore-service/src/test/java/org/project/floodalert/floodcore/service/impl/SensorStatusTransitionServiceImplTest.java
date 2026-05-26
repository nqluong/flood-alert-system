package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.ChangeStatusRequest;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.SensorLogService;
import org.project.floodalert.floodcore.service.SensorRedisCache;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorStatusTransitionServiceImplTest {

    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorLogService sensorLogService;
    @Mock
    private SensorRedisCache sensorRedisCache;

    @InjectMocks
    private SensorStatusTransitionServiceImpl sensorStatusTransitionService;

    private Sensor buildSensor(String status) {
        return Sensor.builder()
                .id(UUID.randomUUID())
                .sensorId("S001")
                .name("Test Sensor")
                .locationName("Quận 12")
                .lat(new BigDecimal("10.5"))
                .lon(new BigDecimal("106.5"))
                .status(status)
                .apiKey("test-api-key")
                .warningThreshold(new BigDecimal("0.5"))
                .dangerThreshold(new BigDecimal("1.0"))
                .build();
    }

    private ChangeStatusRequest buildRequest(String newStatus, String reason) {
        return ChangeStatusRequest.builder().newStatus(newStatus).reason(reason).build();
    }

    private ChangeStatusRequest buildRequest(String newStatus, String reason, String comment) {
        return ChangeStatusRequest.builder().newStatus(newStatus).reason(reason).comment(comment).build();
    }

    // --- changeStatus ---

    @Test
    void changeStatus_success_toDisabled() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("DISABLED", "maintenance"), UUID.randomUUID());
    }

    @Test
    void changeStatus_success_toMaintenance() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("MAINTENANCE"));
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("MAINTENANCE", "scheduled"), UUID.randomUUID());
    }

    @Test
    void changeStatus_success_toOffline() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("OFFLINE"));
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("OFFLINE", "signal lost"), UUID.randomUUID());
    }

    @Test
    void changeStatus_success_disabledToActive() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DISABLED")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("ACTIVE", "restored"), UUID.randomUUID());
    }

    @Test
    void changeStatus_success_commentNotBlank() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("OFFLINE"));
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(),
                buildRequest("OFFLINE", "issue detected", "sensor unreachable"), UUID.randomUUID());
    }

    @Test
    void changeStatus_success_commentBlank() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("OFFLINE"));
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(),
                buildRequest("OFFLINE", "issue", "   "), UUID.randomUUID());
    }

    @Test
    void changeStatus_sensorNotFound() {
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("DISABLED", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatus_sameStatus_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("ACTIVE", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatus_invalidTransition_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DISABLED")));
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("OFFLINE", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatus_deletedSensor_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DELETED")));
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("ACTIVE", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatus_invalidStatusString_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("INVALID_STATUS", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatus_syncSensorToRedisException_returnsFalse() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("MAINTENANCE"));
        doThrow(new RuntimeException("redis error")).when(sensorRedisCache).syncSensorToRedis(any());
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("MAINTENANCE", "reason"), UUID.randomUUID());
    }

    @Test
    void changeStatus_updateSensorStatusException_returnsFalse() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        doThrow(new RuntimeException("redis error")).when(sensorRedisCache).updateSensorStatus(any(), any());
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("DISABLED", "reason"), UUID.randomUUID());
    }

    @Test
    void changeStatus_evictCacheException_swallowed() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        doThrow(new RuntimeException("cache error")).when(sensorRedisCache).evictSensorCache(any());
        sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("DISABLED", "reason"), UUID.randomUUID());
    }

    @Test
    void changeStatus_systemExceptionWrapped() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new RuntimeException("DB error")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatus(UUID.randomUUID(), buildRequest("DISABLED", "reason"), UUID.randomUUID()));
    }

    // --- changeStatusBySensorId ---

    @Test
    void changeStatusBySensorId_success() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(buildSensor("MAINTENANCE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorStatusTransitionService.changeStatusBySensorId("S001", buildRequest("ACTIVE", "recovered"), UUID.randomUUID());
    }

    @Test
    void changeStatusBySensorId_notFound() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatusBySensorId("S001", buildRequest("DISABLED", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatusBySensorId_appExceptionRethrown() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatusBySensorId("S001", buildRequest("ACTIVE", "reason"), UUID.randomUUID()));
    }

    @Test
    void changeStatusBySensorId_systemExceptionWrapped() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new RuntimeException("DB error")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorStatusTransitionService.changeStatusBySensorId("S001", buildRequest("DISABLED", "reason"), UUID.randomUUID()));
    }
}
