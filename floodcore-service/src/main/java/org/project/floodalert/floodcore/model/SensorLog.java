package org.project.floodalert.floodcore.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "sensor_logs")
public class SensorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "sensor_id", nullable = false)
    UUID sensorId;

    @Column(name = "action", nullable = false, length = 50)
    String action; // CREATED, DISABLED, ENABLED, LOCATION_UPDATED

    @Column(name = "performed_by")
    UUID performedBy;

    @Column(name = "old_value", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    Map<String, Object> oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    Map<String, Object> newValue;

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
