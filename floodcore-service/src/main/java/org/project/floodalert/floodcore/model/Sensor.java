package org.project.floodalert.floodcore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "sensors")
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "sensor_id", unique = true, nullable = false, length = 50)
    String sensorId;

    @Column(name = "name", length = 255)
    String name;

    @Column(name = "location_name", columnDefinition = "TEXT")
    String locationName;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal lat;

    @Column(name = "lon", nullable = false, precision = 11, scale = 8)
    BigDecimal lon;

    @Column(name = "status", length = 20)
    String status = "ACTIVE"; // ACTIVE, DISABLED, MAINTENANCE, OFFLINE

    @Column(name = "api_key", unique = true, nullable = false, length = 255)
    String apiKey;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    BigDecimal warningThreshold;

    @Column(name = "danger_threshold", precision = 5, scale = 2)
    BigDecimal dangerThreshold;

    @Column(name = "hardware_model", length = 100)
    String hardwareModel;

    @Column(name = "firmware_version", length = 50)
    String firmwareVersion;

    @Column(name = "battery_level")
    Integer batteryLevel;

    @Column(name = "signal_strength")
    Integer signalStrength;

    @Column(name = "installed_at")
    LocalDateTime installedAt;

    @Column(name = "last_heartbeat")
    LocalDateTime lastHeartbeat;

    @Column(name = "last_reading_at")
    LocalDateTime lastReadingAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "created_by")
    UUID createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
