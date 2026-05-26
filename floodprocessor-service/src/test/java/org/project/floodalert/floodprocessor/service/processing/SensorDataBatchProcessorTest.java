package org.project.floodalert.floodprocessor.service.processing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;
import org.project.floodalert.floodprocessor.dto.request.SensorRaw;
import org.project.floodalert.floodprocessor.service.cache.RedisCacheService;
import org.project.floodalert.floodprocessor.service.core.BusinessLogicService;
import org.project.floodalert.floodprocessor.tracker.SensorTelemetryTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorDataBatchProcessorTest {

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private BusinessLogicService businessLogicService;

    @Mock
    private SensorTelemetryTracker telemetryTracker;

    @InjectMocks
    private SensorDataBatchProcessor processor;

    private SensorRaw.DeviceInfo buildDeviceInfo(String sensorId) {
        return SensorRaw.DeviceInfo.builder()
                .sensorId(sensorId)
                .model("Model-X")
                .firmwareVer("1.0.0")
                .build();
    }

    private SensorRaw.Telemetry buildTelemetry() {
        return SensorRaw.Telemetry.builder()
                .waterLevel(15.0)
                .lat(10.5)
                .lon(106.5)
                .build();
    }

    private SensorRaw.Health buildHealth() {
        return SensorRaw.Health.builder()
                .batteryLevel(85.0)
                .temperature(30.0)
                .signalStrength(-70)
                .status("NORMAL")
                .build();
    }

    private SensorMessage buildMessage(String sensorId, boolean includeHealth) {
        SensorRaw raw = SensorRaw.builder()
                .deviceInfo(buildDeviceInfo(sensorId))
                .telemetry(buildTelemetry())
                .health(includeHealth ? buildHealth() : null)
                .timestamp(System.currentTimeMillis())
                .build();
        return SensorMessage.builder().sensorId(sensorId).sensorData(raw).build();
    }

    private Map<String, String> buildRedisData(String warning, String danger, String street, String district) {
        Map<String, String> data = new HashMap<>();
        if (warning != null) data.put("warning_threshold", warning);
        if (danger != null) data.put("danger_threshold", danger);
        if (street != null) data.put("location_name", street);
        if (district != null) data.put("district", district);
        return data;
    }

    private void stubRedis(String sensorId, Map<String, String> data) {
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of(sensorId, data));
    }

    @Test
    void processBatch_nullMessages_returnsEarly() {
        processor.processBatch(null);
    }

    @Test
    void processBatch_emptyMessages_returnsEarly() {
        processor.processBatch(List.of());
    }

    @Test
    void processBatch_allValid_withHealth_processes() {
        stubRedis("sensor-001", buildRedisData("30.0", "50.0", "Nguyen Hue", "Quan 1"));
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_allValid_healthNull_usesDefaultDeviceStatus() {
        stubRedis("sensor-001", buildRedisData("30.0", "50.0", "Nguyen Hue", null));
        processor.processBatch(List.of(buildMessage("sensor-001", false)));
    }

    @Test
    void processBatch_sensorDataNull_skipsMessage() {
        SensorMessage msg = SensorMessage.builder().sensorId("sensor-001").sensorData(null).build();
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of());
        processor.processBatch(List.of(msg));
    }

    @Test
    void processBatch_deviceInfoNull_skipsMessage() {
        SensorRaw raw = SensorRaw.builder().deviceInfo(null).telemetry(buildTelemetry()).build();
        SensorMessage msg = SensorMessage.builder().sensorId("sensor-001").sensorData(raw).build();
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of());
        processor.processBatch(List.of(msg));
    }

    @Test
    void processBatch_telemetryNull_skipsMessage() {
        SensorRaw raw = SensorRaw.builder().deviceInfo(buildDeviceInfo("sensor-001")).telemetry(null).build();
        SensorMessage msg = SensorMessage.builder().sensorId("sensor-001").sensorData(raw).build();
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of());
        processor.processBatch(List.of(msg));
    }

    @Test
    void processBatch_sensorIdNullInMessage_fallsBackToDeviceInfo() {
        SensorRaw raw = SensorRaw.builder()
                .deviceInfo(buildDeviceInfo("sensor-001"))
                .telemetry(buildTelemetry())
                .health(buildHealth())
                .build();
        SensorMessage msg = SensorMessage.builder().sensorId(null).sensorData(raw).build();
        stubRedis("sensor-001", buildRedisData("30.0", "50.0", null, null));
        processor.processBatch(List.of(msg));
    }

    @Test
    void processBatch_sensorIdNullEverywhere_skipsMessage() {
        SensorRaw raw = SensorRaw.builder()
                .deviceInfo(SensorRaw.DeviceInfo.builder().sensorId(null).build())
                .telemetry(buildTelemetry())
                .build();
        SensorMessage msg = SensorMessage.builder().sensorId(null).sensorData(raw).build();
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of());
        processor.processBatch(List.of(msg));
    }

    @Test
    void processBatch_sensorNotRegisteredInRedis_dropsMessage() {
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of());
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_emptyRedisData_dropsMessage() {
        when(redisCacheService.bulkFetchSensorInfo(anySet())).thenReturn(Map.of("sensor-001", Map.of()));
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_warningThresholdMissing_dropsMessage() {
        stubRedis("sensor-001", buildRedisData(null, "50.0", "Street", "District"));
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_dangerThresholdMissing_dropsMessage() {
        stubRedis("sensor-001", buildRedisData("30.0", null, "Street", "District"));
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_invalidThresholdFormat_dropsMessage() {
        stubRedis("sensor-001", buildRedisData("not-a-number", "50.0", "Street", "District"));
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_onlyDistrict_buildsLocationName() {
        stubRedis("sensor-001", buildRedisData("30.0", "50.0", null, "Quan 1"));
        processor.processBatch(List.of(buildMessage("sensor-001", true)));
    }

    @Test
    void processBatch_redisCacheThrows_rethrowsAsRuntimeException() {
        when(redisCacheService.bulkFetchSensorInfo(anySet()))
                .thenThrow(new RuntimeException("Redis down", new RuntimeException("cause")));
        assertThrows(RuntimeException.class,
                () -> processor.processBatch(List.of(buildMessage("sensor-001", true))));
    }
}
