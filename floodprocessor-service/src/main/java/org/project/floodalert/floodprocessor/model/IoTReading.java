package org.project.floodalert.floodprocessor.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "iot_readings")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IoTReading {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "reading_id", nullable = false, unique = true, length = 50)
    String readingId;

    @Column(name = "sensor_id", nullable = false, length = 50)
    String sensorId;

    @Column(name = "flood_event_id")
    UUID floodEventId;

    @Column(name = "water_level", precision = 5, scale = 2)
    BigDecimal waterLevel;

    @Column(name = "battery_level")
    Integer batteryLevel;

    @Column(name = "signal_strength")
    Integer signalStrength;

    @Column(name = "temperature", precision = 4, scale = 1)
    BigDecimal temperature;

    @Column(name = "humidity", precision = 4, scale = 1)
    BigDecimal humidity;

    @Column(name = "measured_at", nullable = false)
    Instant measuredAt;

    @CreationTimestamp
    @Column(name = "received_at")
    Instant receivedAt;

    @Type(JsonBinaryType.class)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    JsonNode rawPayload;

    @Column(name = "status", length = 20)
    String status;


}
