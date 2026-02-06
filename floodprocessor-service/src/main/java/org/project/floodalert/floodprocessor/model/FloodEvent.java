package org.project.floodalert.floodprocessor.model;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "flood_events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloodEvent {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    String eventId;

    @Column(name = "source", nullable = false, length = 20)
    String source;

    @Column(name = "source_id", length = 100)
    String sourceId;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal lat;

    @Column(name = "lon", nullable = false, precision = 10, scale = 8)
    BigDecimal lon;

    @Column(name = "location_description", columnDefinition = "TEXT")
    String locationDescription;

    @Column(name = "geo_hash", length = 20)
    String geoHash;

    @Column(name = "water_level", precision = 5, scale = 2)
    BigDecimal waterLevel;

    @Column(name = "severity_level", length = 20)
    String severityLevel;

    @Column(name = "status", length = 20)
    String status = "PENDING";

    @Column(name = "confidence_score", precision = 3, scale = 2)
    BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "vote_count")
    Integer voteCount = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    Map<String, Object> rawData;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    String[] tags;

    @Column(name = "processed_at")
    LocalDateTime processedAt;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        processedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
