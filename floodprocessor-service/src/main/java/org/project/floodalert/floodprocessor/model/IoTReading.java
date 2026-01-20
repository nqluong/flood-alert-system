package org.project.floodalert.floodprocessor.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
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
    UUID ID;

    @Column(name = "reading_id", nullable = false, unique = true, length = 50)
    String READING_ID;

    @Column(name = "sensor_id", nullable = false, length = 50)
    String SENSOR_ID;

    @Column(name = "flood_event_id")
    UUID FLOOD_EVENT_ID;

    @Column(name = "water_level", precision = 5, scale = 2)
    BigDecimal WATER_LEVEL;

    @Column(name = "battery_level")
    Integer BATTERY_LEVEL;

    @Column(name = "signal_strength")
    Integer SIGNAL_STRENGTH;

    @Column(name = "temperature", precision = 4, scale = 1)
    BigDecimal TEMPERATURE;

    @Column(name = "humidity", precision = 4, scale = 1)
    BigDecimal HUMIDITY;

    @Column(name = "measured_at", nullable = false)
    LocalDateTime MEASURED_AT;

    @Column(name = "received_at")
    LocalDateTime RECEIVED_AT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    Map<String, Object> RAW_PAYLOAD;

    @PrePersist
    protected void onCreate() {
        RECEIVED_AT = LocalDateTime.now();
    }
}
