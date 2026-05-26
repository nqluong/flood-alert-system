package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.model.Sensor;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SensorChangeTrackingServiceImplTest {

    @InjectMocks
    private SensorChangeTrackingServiceImpl service;

    @Test
    void detectChangedFields_requestAllNull_noChanges() {
        Sensor old = Sensor.builder().name("old").build();
        UpdateSensorRequest req = UpdateSensorRequest.builder().build();
        service.detectChangedFields(old, req);
    }

    @Test
    void detectChangedFields_allFieldsChanged() {
        Sensor old = Sensor.builder()
                .name("old").locationName("old loc")
                .lat(new BigDecimal("10.0")).lon(new BigDecimal("106.0"))
                .warningThreshold(new BigDecimal("0.5")).dangerThreshold(new BigDecimal("1.0"))
                .hardwareModel("old model").firmwareVersion("1.0")
                .build();
        UpdateSensorRequest req = UpdateSensorRequest.builder()
                .name("new").locationName("new loc")
                .lat(new BigDecimal("10.1")).lon(new BigDecimal("106.1"))
                .warningThreshold(new BigDecimal("0.6")).dangerThreshold(new BigDecimal("1.1"))
                .hardwareModel("new model").firmwareVersion("2.0")
                .build();
        service.detectChangedFields(old, req);
    }

    @Test
    void detectChangedFields_oldValueNull_newNotNull_changesDetected() {
        Sensor old = Sensor.builder().build();
        UpdateSensorRequest req = UpdateSensorRequest.builder()
                .name("new").lat(new BigDecimal("10.5"))
                .build();
        service.detectChangedFields(old, req);
    }

    @Test
    void detectChangedFields_sameValues_noChange() {
        BigDecimal val = new BigDecimal("10.5");
        Sensor old = Sensor.builder().name("same").lat(val).build();
        UpdateSensorRequest req = UpdateSensorRequest.builder().name("same").lat(val).build();
        service.detectChangedFields(old, req);
    }

    @Test
    void createOldValueSnapshot_allKnownFields_nonNullValues() {
        Sensor sensor = Sensor.builder()
                .name("test").locationName("loc")
                .lat(new BigDecimal("10.5")).lon(new BigDecimal("106.5"))
                .warningThreshold(new BigDecimal("0.5")).dangerThreshold(new BigDecimal("1.0"))
                .hardwareModel("model").firmwareVersion("1.0").status("ACTIVE")
                .build();
        List<String> fields = List.of(
                "name", "locationName", "lat", "lon",
                "warningThreshold", "dangerThreshold", "hardwareModel", "firmwareVersion", "status");
        service.createOldValueSnapshot(sensor, fields);
    }

    @Test
    void createOldValueSnapshot_unknownField_defaultCaseSkipped() {
        service.createOldValueSnapshot(Sensor.builder().build(), List.of("unknownField"));
    }

    @Test
    void createOldValueSnapshot_fieldValueNull_notAddedToSnapshot() {
        Sensor sensor = Sensor.builder().build();
        service.createOldValueSnapshot(sensor, List.of("name"));
    }

    @Test
    void createNewValueSnapshot_withChangedFields() {
        Sensor sensor = Sensor.builder()
                .name("test").lat(new BigDecimal("10.5"))
                .build();
        service.createNewValueSnapshot(sensor, List.of("name", "lat", "unknownField"));
    }

    @Test
    void isLocationUpdate_containsLat_returnsTrue() {
        service.isLocationUpdate(List.of("lat", "name"));
    }

    @Test
    void isLocationUpdate_containsLon_returnsTrue() {
        service.isLocationUpdate(List.of("lon"));
    }

    @Test
    void isLocationUpdate_noLocationFields_returnsFalse() {
        service.isLocationUpdate(List.of("name", "hardwareModel"));
    }

    @Test
    void isThresholdUpdate_containsWarningThreshold_returnsTrue() {
        service.isThresholdUpdate(List.of("warningThreshold"));
    }

    @Test
    void isThresholdUpdate_containsDangerThreshold_returnsTrue() {
        service.isThresholdUpdate(List.of("dangerThreshold"));
    }

    @Test
    void isThresholdUpdate_noThresholdFields_returnsFalse() {
        service.isThresholdUpdate(List.of("name"));
    }
}
