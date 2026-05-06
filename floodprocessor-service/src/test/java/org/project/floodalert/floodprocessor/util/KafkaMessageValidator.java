package org.project.floodalert.floodprocessor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.request.SensorMessage;

/**
 * Utility class để validate Kafka message format
 * Dùng để debug và test message structure
 */
@Slf4j
public class KafkaMessageValidator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate message structure và log chi tiết
     */
    public static boolean validateMessage(String jsonMessage) {
        try {
            SensorMessage message = objectMapper.readValue(jsonMessage, SensorMessage.class);
            
            log.info("=== Kafka Message Validation ===");
            log.info("Top-level sensorId: {}", message.getSensorId());
            log.info("SensorData is null: {}", message.getSensorData() == null);
            
            if (message.getSensorData() != null) {
                log.info("DeviceInfo is null: {}", message.getSensorData().getDeviceInfo() == null);
                log.info("Telemetry is null: {}", message.getSensorData().getTelemetry() == null);
                log.info("Health is null: {}", message.getSensorData().getHealth() == null);
                
                if (message.getSensorData().getDeviceInfo() != null) {
                    log.info("DeviceInfo.sensorId: {}", 
                            message.getSensorData().getDeviceInfo().getSensorId());
                }
                
                if (message.getSensorData().getTelemetry() != null) {
                    log.info("Telemetry.waterLevel: {}", 
                            message.getSensorData().getTelemetry().getWaterLevel());
                    log.info("Telemetry.lat: {}", 
                            message.getSensorData().getTelemetry().getLat());
                    log.info("Telemetry.lon: {}", 
                            message.getSensorData().getTelemetry().getLon());
                }
            }
            
            log.info("Topic: {}", message.getTopic());
            log.info("ReceivedAt: {}", message.getReceivedAt());
            log.info("=== Validation Result: VALID ===");
            
            return true;
            
        } catch (Exception e) {
            log.error("=== Validation Result: INVALID ===");
            log.error("Error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate sample valid message
     */
    public static String generateSampleMessage() {
        return """
                {
                  "sensorId": "SENS-HAN-000",
                  "sensorData": {
                    "device_info": {
                      "sensor_id": "SENS-HAN-000",
                      "model": "FG-PRO-ULTRA",
                      "firmware_ver": "2.1.0",
                      "message_id": "msg-f6434d0e-b3cb-4d23-a9e4-0b1db04383ed"
                    },
                    "telemetry": {
                      "water_level": 1.95,
                      "distance_raw": 418.05,
                      "velocity": 0.0,
                      "lat": 20.998051,
                      "lon": 105.840337
                    },
                    "health": {
                      "battery_level": 93.1,
                      "temperature": 32.1,
                      "signal_strength": -68,
                      "status": "ACTIVE"
                    },
                    "timestamp": 1777997427819
                  },
                  "topic": "floodguard/sensors/SENS-HAN-000/telemetry",
                  "receivedAt": 1777997428.760802000
                }
                """;
    }

    public static void main(String[] args) {
        // Test với sample message
        String sampleMessage = generateSampleMessage();
        validateMessage(sampleMessage);
        
        // Test với message thiếu sensorId
        String invalidMessage = """
                {
                  "sensorData": {
                    "device_info": {
                      "sensor_id": "SENS-HAN-000"
                    }
                  }
                }
                """;
        validateMessage(invalidMessage);
    }
}
