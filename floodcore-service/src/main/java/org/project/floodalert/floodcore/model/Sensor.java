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
    UUID ID;

    @Column(name = "sensor_id", unique = true, nullable = false, length = 50)
    String SENSOR_ID;

    @Column(name = "name", length = 255)
    String NAME;

    @Column(name = "location_name", columnDefinition = "TEXT")
    String LOCATION_NAME;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal LAT;

    @Column(name = "lon", nullable = false, precision = 11, scale = 8)
    BigDecimal LON;

    @Column(name = "status", length = 20)
    String STATUS = "ACTIVE"; // ACTIVE, DISABLED, MAINTENANCE, OFFLINE

    @Column(name = "api_key", unique = true, nullable = false, length = 255)
    String API_KEY;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    BigDecimal WARNING_THRESHOLD;

    @Column(name = "danger_threshold", precision = 5, scale = 2)
    BigDecimal DANGER_THRESHOLD;

    @Column(name = "hardware_model", length = 100)
    String HARDWARE_MODEL;

    @Column(name = "firmware_version", length = 50)
    String FIRMWARE_VERSION;

    @Column(name = "battery_level")
    Integer BATTERY_LEVEL;

    @Column(name = "signal_strength")
    Integer SIGNAL_STRENGTH;

    @Column(name = "installed_at")
    LocalDateTime INSTALLED_AT;

    @Column(name = "last_heartbeat")
    LocalDateTime LAST_HEARTBEAT;

    @Column(name = "last_reading_at")
    LocalDateTime LAST_READING_AT;

    @Column(name = "created_at", updatable = false)
    LocalDateTime CREATED_AT;

    @Column(name = "updated_at")
    LocalDateTime UPDATED_AT;

    @Column(name = "created_by")
    UUID CREATED_BY;

    @PrePersist
    protected void onCreate() {
        CREATED_AT = LocalDateTime.now();
        UPDATED_AT = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        UPDATED_AT = LocalDateTime.now();
    }
}
