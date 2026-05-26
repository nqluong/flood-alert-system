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
import org.project.floodalert.floodcore.dto.request.DeleteSensorRequest;
import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.dto.response.ChangeStatusResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.SensorChangeTrackingService;
import org.project.floodalert.floodcore.service.SensorLogService;
import org.project.floodalert.floodcore.service.SensorRedisCache;
import org.project.floodalert.floodcore.service.SensorStatusTransitionService;
import org.project.floodalert.floodcore.service.SensorValidationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorUpdateServiceImplTest {

    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorValidationService sensorValidationService;
    @Mock
    private SensorChangeTrackingService sensorChangeTrackingService;
    @Mock
    private SensorLogService sensorLogService;
    @Mock
    private SensorRedisCache sensorRedisCache;
    @Mock
    private SensorStatusTransitionService sensorStatusTransitionService;

    @InjectMocks
    private SensorUpdateServiceImpl sensorUpdateService;

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
                .hardwareModel("Model A")
                .firmwareVersion("1.0.0")
                .build();
    }

    private DeleteSensorRequest buildDeleteRequest(String reason, boolean removeFromMap) {
        return DeleteSensorRequest.builder().reason(reason).removeFromMap(removeFromMap).build();
    }

    @Test
    void updateSensor_noChanges_returnsEarlyResponse() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of());
        sensorUpdateService.updateSensor(UUID.randomUUID(), UpdateSensorRequest.builder().build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_basicFields() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("name"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of("name", "old"));
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of("name", "new"));
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder().name("new name").build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_withComment() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("name"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder().name("new").comment("admin note").build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_allFields() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any()))
                .thenReturn(List.of("name", "locationName", "lat", "lon",
                        "warningThreshold", "dangerThreshold", "hardwareModel", "firmwareVersion"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder()
                        .name("new name").locationName("new loc")
                        .lat(new BigDecimal("10.6")).lon(new BigDecimal("106.6"))
                        .warningThreshold(new BigDecimal("0.6")).dangerThreshold(new BigDecimal("1.1"))
                        .hardwareModel("new model").firmwareVersion("2.0")
                        .build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_withCoordinatesValidation() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("lat", "lon"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder()
                        .lat(new BigDecimal("10.6")).lon(new BigDecimal("106.6"))
                        .build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_withThresholdValidation() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("warningThreshold", "dangerThreshold"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder()
                        .warningThreshold(new BigDecimal("0.6")).dangerThreshold(new BigDecimal("1.1"))
                        .build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_statusChangesToBlacklist() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("name"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder().name("new").build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_success_statusChangesFromBlacklist() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DISABLED")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("name"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder().name("new").build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_evictCacheException_swallowed() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("name"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        doThrow(new RuntimeException("cache error")).when(sensorRedisCache).evictSensorCache(any());
        sensorUpdateService.updateSensor(UUID.randomUUID(),
                UpdateSensorRequest.builder().name("new").build(), UUID.randomUUID());
    }

    @Test
    void updateSensor_sensorNotFound_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.updateSensor(UUID.randomUUID(), UpdateSensorRequest.builder().build(), UUID.randomUUID()));
    }

    @Test
    void updateSensor_appExceptionRethrown() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("lat"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        doThrow(new AppException(CoreErrorCode.SENSOR_NOT_FOUND, "invalid coords"))
                .when(sensorValidationService).validateCoordinates(any(), any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.updateSensor(UUID.randomUUID(),
                        UpdateSensorRequest.builder().lat(new BigDecimal("10.6")).build(), UUID.randomUUID()));
    }

    @Test
    void updateSensor_systemExceptionWrapped() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of("name"));
        when(sensorChangeTrackingService.createOldValueSnapshot(any(), any())).thenReturn(Map.of());
        when(sensorChangeTrackingService.createNewValueSnapshot(any(), any())).thenReturn(Map.of());
        doThrow(new RuntimeException("DB error")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.updateSensor(UUID.randomUUID(),
                        UpdateSensorRequest.builder().name("new").build(), UUID.randomUUID()));
    }

    @Test
    void updateSensorBySensorId_found() {
        Sensor sensor = buildSensor("ACTIVE");
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(sensor));
        when(sensorRepository.findById(any())).thenReturn(Optional.of(sensor));
        when(sensorChangeTrackingService.detectChangedFields(any(), any())).thenReturn(List.of());
        sensorUpdateService.updateSensorBySensorId("S001", UpdateSensorRequest.builder().build(), UUID.randomUUID());
    }

    @Test
    void updateSensorBySensorId_notFound() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.updateSensorBySensorId("S001", UpdateSensorRequest.builder().build(), UUID.randomUUID()));
    }

    @Test
    void softDelete_removeFromMap_true_success() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID());
    }

    @Test
    void softDelete_removeFromMap_false_success() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", false), UUID.randomUUID());
    }

    @Test
    void softDelete_alreadyDeleted_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DELETED")));
        assertThrows(AppException.class, () ->
                sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", false), UUID.randomUUID()));
    }

    @Test
    void softDelete_alreadyDisabled_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DISABLED")));
        assertThrows(AppException.class, () ->
                sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", false), UUID.randomUUID()));
    }

    @Test
    void softDelete_sensorNotFound_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", false), UUID.randomUUID()));
    }

    @Test
    void softDelete_appExceptionRethrown() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new AppException(CoreErrorCode.DATABASE_ERROR, "test")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", false), UUID.randomUUID()));
    }

    @Test
    void softDelete_systemExceptionWrapped() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new RuntimeException("DB error")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", false), UUID.randomUUID()));
    }

    @Test
    void softDelete_updateRedisException_swallowed() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        doThrow(new RuntimeException("redis error")).when(sensorRedisCache).removeSensorFromRedis(any());
        sensorUpdateService.softDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID());
    }

    @Test
    void softDeleteBySensorId_found() {
        Sensor sensor = buildSensor("ACTIVE");
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(sensor));
        when(sensorRepository.findById(any())).thenReturn(Optional.of(sensor));
        when(sensorRepository.save(any())).thenReturn(buildSensor("DISABLED"));
        sensorUpdateService.softDeleteBySensorId("S001", buildDeleteRequest("reason", false), UUID.randomUUID());
    }

    @Test
    void softDeleteBySensorId_notFound() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.softDeleteBySensorId("S001", buildDeleteRequest("reason", false), UUID.randomUUID()));
    }

    @Test
    void hardDelete_success() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        sensorUpdateService.hardDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID());
    }

    @Test
    void hardDelete_removeFromRedisException_swallowed() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new RuntimeException("redis error")).when(sensorRedisCache).removeSensorFromRedis(any());
        sensorUpdateService.hardDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID());
    }

    @Test
    void hardDelete_sensorNotFound() {
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.hardDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID()));
    }

    @Test
    void hardDelete_appExceptionRethrown() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new AppException(CoreErrorCode.DATABASE_ERROR, "test"))
                .when(sensorLogService).logWithComment(any(), any(), any(), any(), any(), any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.hardDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID()));
    }

    @Test
    void hardDelete_systemExceptionWrapped() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        doThrow(new RuntimeException("DB error")).when(sensorRepository).delete(any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.hardDelete(UUID.randomUUID(), buildDeleteRequest("reason", true), UUID.randomUUID()));
    }

    @Test
    void hardDeleteBySensorId_found() {
        Sensor sensor = buildSensor("ACTIVE");
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(sensor));
        when(sensorRepository.findById(any())).thenReturn(Optional.of(sensor));
        sensorUpdateService.hardDeleteBySensorId("S001", buildDeleteRequest("reason", true), UUID.randomUUID());
    }

    @Test
    void hardDeleteBySensorId_notFound() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.hardDeleteBySensorId("S001", buildDeleteRequest("reason", true), UUID.randomUUID()));
    }


    @Test
    void restoreSensor_fromDeleted_success() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DELETED")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.restoreSensor(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void restoreSensor_fromDisabled_success() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DISABLED")));
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.restoreSensor(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void restoreSensor_notDeletedOrDisabled_throwsAppException() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("ACTIVE")));
        assertThrows(AppException.class, () ->
                sensorUpdateService.restoreSensor(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void restoreSensor_sensorNotFound() {
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.restoreSensor(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void restoreSensor_appExceptionRethrown() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DELETED")));
        doThrow(new AppException(CoreErrorCode.DATABASE_ERROR, "test")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.restoreSensor(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void restoreSensor_systemExceptionWrapped() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor("DISABLED")));
        doThrow(new RuntimeException("DB error")).when(sensorRepository).save(any());
        assertThrows(AppException.class, () ->
                sensorUpdateService.restoreSensor(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void restoreSensorBySensorId_found() {
        Sensor sensor = buildSensor("DELETED");
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(sensor));
        when(sensorRepository.findById(any())).thenReturn(Optional.of(sensor));
        when(sensorRepository.save(any())).thenReturn(buildSensor("ACTIVE"));
        sensorUpdateService.restoreSensorBySensorId("S001", UUID.randomUUID());
    }

    @Test
    void restoreSensorBySensorId_notFound() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () ->
                sensorUpdateService.restoreSensorBySensorId("S001", UUID.randomUUID()));
    }

    @Test
    void changeStatus_delegatesToService() {
        when(sensorStatusTransitionService.changeStatus(any(), any(), any())).thenReturn(new ChangeStatusResponse());
        sensorUpdateService.changeStatus(UUID.randomUUID(), ChangeStatusRequest.builder().build(), UUID.randomUUID());
    }

    @Test
    void changeStatusBySensorId_delegatesToService() {
        when(sensorStatusTransitionService.changeStatusBySensorId(anyString(), any(), any())).thenReturn(new ChangeStatusResponse());
        sensorUpdateService.changeStatusBySensorId("S001", ChangeStatusRequest.builder().build(), UUID.randomUUID());
    }
}
