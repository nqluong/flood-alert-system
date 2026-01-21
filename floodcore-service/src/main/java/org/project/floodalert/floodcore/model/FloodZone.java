package org.project.floodalert.floodcore.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "flood_zones")
public class FloodZone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID ID;

    @Column(name = "name", nullable = false, length = 255)
    String NAME;

    @Column(name = "description", columnDefinition = "TEXT")
    String DESCRIPTION;

    @Column(name = "center_lat", nullable = false, precision = 10, scale = 8)
    BigDecimal CENTER_LAT;

    @Column(name = "center_lon", nullable = false, precision = 11, scale = 8)
    BigDecimal CENTER_LON;

    @Column(name = "radius_meters", nullable = false)
    Integer RADIUS_METERS = 100;

    @Column(name = "boundary", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    Map<String, Object> BOUNDARY;

    @Column(name = "level", length = 20)
    String LEVEL; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "is_active")
    Boolean IS_ACTIVE = true;

    @Column(name = "is_pinned")
    Boolean IS_PINNED = false;

    @Column(name = "created_by")
    UUID CREATED_BY;

    @Column(name = "created_at", updatable = false)
    LocalDateTime CREATED_AT;

    @Column(name = "update_at")
    LocalDateTime UPDATED_AT;

    @Column(name = "expires_at")
    LocalDateTime EXPIRES_AT;

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
