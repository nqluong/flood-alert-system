package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorLogRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SensorLogServiceImplTest {

    @Mock
    private SensorLogRepository sensorLogRepository;

    @InjectMocks
    private SensorLogServiceImpl sensorLogService;

    private Sensor buildSensor() {
        return Sensor.builder()
                .id(UUID.randomUUID())
                .sensorId("S001")
                .name("Test Sensor")
                .locationName("Quận 12")
                .lat(new BigDecimal("10.5"))
                .lon(new BigDecimal("106.5"))
                .status("ACTIVE")
                .warningThreshold(new BigDecimal("0.5"))
                .dangerThreshold(new BigDecimal("1.0"))
                .hardwareModel("Model A")
                .firmwareVersion("1.0.0")
                .build();
    }

    @Test
    void logSensorCreated_savesLog() {
        sensorLogService.logSensorCreated(buildSensor(), UUID.randomUUID());
    }

    @Test
    void logSensorUpdated_savesLog() {
        sensorLogService.logSensorUpdated(
                UUID.randomUUID(), "UPDATED",
                Map.of("name", "old"), Map.of("name", "new"),
                UUID.randomUUID()
        );
    }

    @Test
    void logWithComment_savesLog() {
        sensorLogService.logWithComment(
                UUID.randomUUID(), "STATUS_CHANGED",
                Map.of("status", "ACTIVE"), Map.of("status", "DISABLED"),
                "Admin disabled sensor", UUID.randomUUID()
        );
    }
}
