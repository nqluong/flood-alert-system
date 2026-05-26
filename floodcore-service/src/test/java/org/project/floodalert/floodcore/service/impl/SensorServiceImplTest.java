package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.BatchCreateSensorRequest;
import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.response.CreateSensorResponse;
import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.mapper.SensorMapper;
import org.project.floodalert.floodcore.mapper.SensorReadMapper;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorLogRepository;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.ApiKeyGeneratorService;
import org.project.floodalert.floodcore.service.SensorLogService;
import org.project.floodalert.floodcore.service.SensorRedisCache;
import org.project.floodalert.floodcore.service.SensorValidationService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorServiceImplTest {

    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorLogRepository sensorLogRepository;
    @Mock
    private SensorValidationService sensorValidationService;
    @Mock
    private ApiKeyGeneratorService apiKeyGeneratorService;
    @Mock
    private SensorLogService sensorLogService;
    @Mock
    private SensorRedisCache sensorRedisCache;
    @Mock
    private SensorMapper sensorMapper;
    @Mock
    private SensorReadMapper readMapper;

    @InjectMocks
    private SensorServiceImpl sensorService;

    private Sensor buildSensor() {
        return Sensor.builder()
                .id(UUID.randomUUID())
                .sensorId("S001")
                .name("Test Sensor")
                .locationName("Quận 12")
                .lat(new BigDecimal("10.5"))
                .lon(new BigDecimal("106.5"))
                .status("ACTIVE")
                .apiKey("test-api-key")
                .warningThreshold(new BigDecimal("0.5"))
                .dangerThreshold(new BigDecimal("1.0"))
                .hardwareModel("Model A")
                .firmwareVersion("1.0.0")
                .build();
    }

    private void setupCreateSensorSuccess() {
        when(apiKeyGeneratorService.generateUniqueApiKey()).thenReturn("api-key");
        when(sensorMapper.toEntity(any(), anyString())).thenReturn(buildSensor());
        when(sensorRepository.save(any())).thenReturn(buildSensor());
        when(sensorMapper.toCreateResponse(any())).thenReturn(new CreateSensorResponse());
    }

    @Test
    void createSensor_success() {
        setupCreateSensorSuccess();
        sensorService.createSensor(CreateSensorRequest.builder().sensorId("S001").build(), UUID.randomUUID());
    }

    @Test
    void createSensor_appExceptionRethrown() {
        doThrow(new AppException(CoreErrorCode.SENSOR_NOT_FOUND, "test"))
                .when(sensorValidationService).validateCreateRequest(any());
        assertThrows(AppException.class, () -> sensorService
                .createSensor(CreateSensorRequest.builder().sensorId("S001").build(), UUID.randomUUID()));
    }

    @Test
    void createSensor_systemExceptionWrapped() {
        doThrow(new RuntimeException("DB error"))
                .when(sensorValidationService).validateCreateRequest(any());
        assertThrows(AppException.class, () -> sensorService
                .createSensor(CreateSensorRequest.builder().sensorId("S001").build(), UUID.randomUUID()));
    }

    @Test
    void batchCreateSensors_allSuccess() {
        setupCreateSensorSuccess();
        BatchCreateSensorRequest batchRequest = BatchCreateSensorRequest.builder()
                .sensors(List.of(CreateSensorRequest.builder().sensorId("S001").build()))
                .build();
        sensorService.batchCreateSensors(batchRequest, UUID.randomUUID());
    }

    @Test
    void batchCreateSensors_withFailure() {
        doThrow(new RuntimeException("fail")).when(sensorValidationService).validateCreateRequest(any());
        BatchCreateSensorRequest batchRequest = BatchCreateSensorRequest.builder()
                .sensors(List.of(CreateSensorRequest.builder().sensorId("S001").build()))
                .build();
        sensorService.batchCreateSensors(batchRequest, UUID.randomUUID());
    }

    @Test
    void batchCreateSensors_mixedResults() {
        doThrow(new RuntimeException("fail")).doNothing()
                .when(sensorValidationService).validateCreateRequest(any());
        setupCreateSensorSuccess();
        BatchCreateSensorRequest batchRequest = BatchCreateSensorRequest.builder()
                .sensors(List.of(
                        CreateSensorRequest.builder().sensorId("S001").build(),
                        CreateSensorRequest.builder().sensorId("S002").build()))
                .build();
        sensorService.batchCreateSensors(batchRequest, UUID.randomUUID());
    }

    @Test
    void getSensorList_cacheHit() {
        when(sensorRedisCache.getCachedSensorList(any())).thenReturn(Optional.of(new PageResponse<>()));
        sensorService.getSensorList(SensorFilterRequest.builder().build());
    }

    @Test
    void getSensorList_cacheMiss_noFilter() {
        when(sensorRedisCache.getCachedSensorList(any())).thenReturn(Optional.empty());
        when(sensorRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(buildSensor())));
        when(readMapper.toSummaryResponse(any())).thenReturn(new SensorSummaryResponse());
        sensorService.getSensorList(SensorFilterRequest.builder().build());
    }

    @Test
    void getSensorList_cacheMiss_onlyStatus() {
        when(sensorRedisCache.getCachedSensorList(any())).thenReturn(Optional.empty());
        when(sensorRepository.findByStatus(anyString(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        sensorService.getSensorList(SensorFilterRequest.builder().status("ACTIVE").build());
    }

    @Test
    void getSensorList_cacheMiss_onlySearch() {
        when(sensorRedisCache.getCachedSensorList(any())).thenReturn(Optional.empty());
        when(sensorRepository.searchByNameOrSensorId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        sensorService.getSensorList(SensorFilterRequest.builder().search("test").build());
    }

    @Test
    void getSensorList_cacheMiss_statusAndSearch() {
        when(sensorRedisCache.getCachedSensorList(any())).thenReturn(Optional.empty());
        when(sensorRepository.findByStatusAndSearch(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        sensorService.getSensorList(SensorFilterRequest.builder().status("ACTIVE").search("test").build());
    }

    @Test
    void getSensorList_sortAscending() {
        when(sensorRedisCache.getCachedSensorList(any())).thenReturn(Optional.empty());
        when(sensorRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        sensorService.getSensorList(SensorFilterRequest.builder().sortDirection("ASC").build());
    }

    @Test
    void getSensorDetail_includeLogs_false_cacheHit() {
        when(sensorRedisCache.getCachedSensorDetail(any())).thenReturn(Optional.of(new SensorDetailResponse()));
        sensorService.getSensorDetail(UUID.randomUUID(), false);
    }

    @Test
    void getSensorDetail_includeLogs_false_cacheMiss() {
        when(sensorRedisCache.getCachedSensorDetail(any())).thenReturn(Optional.empty());
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor()));
        when(readMapper.toDetailResponse(any())).thenReturn(new SensorDetailResponse());
        sensorService.getSensorDetail(UUID.randomUUID(), false);
    }

    @Test
    void getSensorDetail_includeLogs_false_notFound() {
        when(sensorRedisCache.getCachedSensorDetail(any())).thenReturn(Optional.empty());
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> sensorService.getSensorDetail(UUID.randomUUID(), false));
    }

    @Test
    void getSensorDetail_includeLogs_true_success() {
        when(sensorRepository.findById(any())).thenReturn(Optional.of(buildSensor()));
        SensorDetailResponse response = new SensorDetailResponse();
        when(readMapper.toDetailResponse(any())).thenReturn(response);
        when(sensorLogRepository.findTop10BySensorIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(readMapper.toLogResponses(any())).thenReturn(List.of());
        sensorService.getSensorDetail(UUID.randomUUID(), true);
    }

    @Test
    void getSensorDetail_includeLogs_true_notFound() {
        when(sensorRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> sensorService.getSensorDetail(UUID.randomUUID(), true));
    }

    @Test
    void getSensorDetailBySensorId_found() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.of(buildSensor()));
        when(sensorRedisCache.getCachedSensorDetail(any())).thenReturn(Optional.of(new SensorDetailResponse()));
        sensorService.getSensorDetailBySensorId("S001", false);
    }

    @Test
    void getSensorDetailBySensorId_notFound() {
        when(sensorRepository.findBySensorId(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> sensorService.getSensorDetailBySensorId("S001", false));
    }

    @Test
    void getSensorMapData_cacheHit() {
        when(sensorRedisCache.getCachedMapData()).thenReturn(Optional.of(new SensorMapResponse()));
        sensorService.getSensorMapData();
    }

    @Test
    void getSensorMapData_cacheMiss() {
        when(sensorRedisCache.getCachedMapData()).thenReturn(Optional.empty());
        when(sensorRepository.findAll()).thenReturn(List.of(buildSensor()));
        when(readMapper.toMapResponse(any())).thenReturn(new SensorMapResponse());
        sensorService.getSensorMapData();
    }

    @Test
    void getActiveSensorMapData_success() {
        when(sensorRepository.findAllActiveSensors()).thenReturn(List.of(buildSensor()));
        when(readMapper.toMapResponse(any())).thenReturn(new SensorMapResponse());
        sensorService.getActiveSensorMapData();
    }
}
