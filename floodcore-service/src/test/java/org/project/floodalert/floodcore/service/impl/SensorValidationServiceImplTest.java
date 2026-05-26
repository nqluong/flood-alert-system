package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.repository.SensorRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorValidationServiceImplTest {

    @Mock
    private SensorRepository sensorRepository;

    @InjectMocks
    private SensorValidationServiceImpl sensorValidationService;

    @Test
    void validateSensorIdNotExists_notExists_noException() {
        when(sensorRepository.existsBySensorId(anyString())).thenReturn(false);
        sensorValidationService.validateSensorIdNotExists("S001");
    }

    @Test
    void validateSensorIdNotExists_exists_throwsAppException() {
        when(sensorRepository.existsBySensorId(anyString())).thenReturn(true);
        assertThrows(AppException.class, () -> sensorValidationService.validateSensorIdNotExists("S001"));
    }

    @Test
    void validateThresholds_dangerGreaterThanWarning_valid() {
        sensorValidationService.validateThresholds(new BigDecimal("0.5"), new BigDecimal("1.0"));
    }

    @Test
    void validateThresholds_dangerEqualsWarning_throwsAppException() {
        assertThrows(AppException.class, () ->
                sensorValidationService.validateThresholds(new BigDecimal("1.0"), new BigDecimal("1.0")));
    }

    @Test
    void validateThresholds_dangerLessThanWarning_throwsAppException() {
        assertThrows(AppException.class, () ->
                sensorValidationService.validateThresholds(new BigDecimal("1.0"), new BigDecimal("0.5")));
    }

    @Test
    void validateCoordinates_valid_noException() {
        sensorValidationService.validateCoordinates(new BigDecimal("10.5"), new BigDecimal("106.5"));
    }

    @Test
    void validateCoordinates_boundaryValues_valid() {
        sensorValidationService.validateCoordinates(new BigDecimal("-90"), new BigDecimal("-180"));
        sensorValidationService.validateCoordinates(new BigDecimal("90"), new BigDecimal("180"));
    }

    @Test
    void validateCoordinates_latTooSmall_throwsAppException() {
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCoordinates(new BigDecimal("-91"), new BigDecimal("106.5")));
    }

    @Test
    void validateCoordinates_latTooLarge_throwsAppException() {
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCoordinates(new BigDecimal("91"), new BigDecimal("106.5")));
    }

    @Test
    void validateCoordinates_lonTooSmall_throwsAppException() {
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCoordinates(new BigDecimal("10.5"), new BigDecimal("-181")));
    }

    @Test
    void validateCoordinates_lonTooLarge_throwsAppException() {
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCoordinates(new BigDecimal("10.5"), new BigDecimal("181")));
    }

    // --- validateCreateRequest ---

    @Test
    void validateCreateRequest_valid_callsAllValidations() {
        when(sensorRepository.existsBySensorId(anyString())).thenReturn(false);
        sensorValidationService.validateCreateRequest(CreateSensorRequest.builder()
                .sensorId("S001")
                .lat(new BigDecimal("10.5")).lon(new BigDecimal("106.5"))
                .warningThreshold(new BigDecimal("0.5")).dangerThreshold(new BigDecimal("1.0"))
                .build());
    }

    @Test
    void validateCreateRequest_sensorIdExists_throwsAppException() {
        when(sensorRepository.existsBySensorId(anyString())).thenReturn(true);
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCreateRequest(CreateSensorRequest.builder()
                        .sensorId("S001")
                        .lat(new BigDecimal("10.5")).lon(new BigDecimal("106.5"))
                        .warningThreshold(new BigDecimal("0.5")).dangerThreshold(new BigDecimal("1.0"))
                        .build()));
    }

    @Test
    void validateCreateRequest_invalidThreshold_throwsAppException() {
        when(sensorRepository.existsBySensorId(anyString())).thenReturn(false);
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCreateRequest(CreateSensorRequest.builder()
                        .sensorId("S001")
                        .lat(new BigDecimal("10.5")).lon(new BigDecimal("106.5"))
                        .warningThreshold(new BigDecimal("1.0")).dangerThreshold(new BigDecimal("0.5"))
                        .build()));
    }

    @Test
    void validateCreateRequest_invalidCoordinates_throwsAppException() {
        when(sensorRepository.existsBySensorId(anyString())).thenReturn(false);
        assertThrows(AppException.class, () ->
                sensorValidationService.validateCreateRequest(CreateSensorRequest.builder()
                        .sensorId("S001")
                        .lat(new BigDecimal("91")).lon(new BigDecimal("106.5"))
                        .warningThreshold(new BigDecimal("0.5")).dangerThreshold(new BigDecimal("1.0"))
                        .build()));
    }
}
