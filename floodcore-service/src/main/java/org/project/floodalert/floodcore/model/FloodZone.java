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
    UUID id;

    @Column(name = "name", nullable = false, length = 255)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "center_lat", nullable = false, precision = 10, scale = 8)
    BigDecimal centerLat;

    @Column(name = "center_lon", nullable = false, precision = 11, scale = 8)
    BigDecimal centerLon;

    @Column(name = "radius_meters", nullable = false)
    Integer radiusMeters = 100;

    @Column(name = "boundary", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    Map<String, Object> boundary;

    @Column(name = "level", length = 20)
    String level; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "is_active")
    Boolean isActive = true;

    @Column(name = "is_pinned")
    Boolean isPinned = false;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "update_at")
    LocalDateTime updatedAt;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

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
