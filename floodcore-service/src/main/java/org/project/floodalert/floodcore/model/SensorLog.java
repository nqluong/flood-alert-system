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
    UUID ID;

    @Column(name = "sensor_id", nullable = false)
    UUID SENSOR_ID;

    @Column(name = "action", nullable = false, length = 50)
    String ACTION; // CREATED, DISABLED, ENABLED, LOCATION_UPDATED

    @Column(name = "performed_by")
    UUID PERFORMED_BY;

    @Column(name = "old_value", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    Map<String, Object> OLD_VALUE;

    @Column(name = "new_value", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    Map<String, Object> NEW_VALUE;

    @Column(name = "comment", columnDefinition = "TEXT")
    String COMMENT;

    @Column(name = "created_at", updatable = false)
    LocalDateTime CREATED_AT;

    @PrePersist
    protected void onCreate() {
        CREATED_AT = LocalDateTime.now();
    }
}
