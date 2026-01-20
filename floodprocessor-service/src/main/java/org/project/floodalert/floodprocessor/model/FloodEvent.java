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
    UUID ID;

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    String EVENT_ID;

    @Column(name = "source", nullable = false, length = 20)
    String SOURCE;

    @Column(name = "source_id", length = 100)
    String SOURCE_ID;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal LAT;

    @Column(name = "lon", nullable = false, precision = 10, scale = 8)
    BigDecimal LON;

    @Column(name = "location_description", columnDefinition = "TEXT")
    String LOCATION_DESCRIPTION;

    @Column(name = "geo_hash", length = 20)
    String GEO_HASH;

    @Column(name = "water_level", precision = 5, scale = 2)
    BigDecimal WATER_LEVEL;

    @Column(name = "severity_level", length = 20)
    String SEVERITY_LEVEL;

    @Column(name = "status", length = 20)
    String STATUS = "PENDING";

    @Column(name = "confidence_score", precision = 3, scale = 2)
    BigDecimal CONFIDENCE_SCORE = BigDecimal.ZERO;

    @Column(name = "vote_count")
    Integer VOTE_COUNT = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    Map<String, Object> RAW_DATA;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    String[] TAGS;

    @Column(name = "processed_at")
    LocalDateTime PROCESSED_AT;

    @Column(name = "confirmed_at")
    LocalDateTime CONFIRMED_AT;

    @Column(name = "expires_at")
    LocalDateTime EXPIRES_AT;

    @Column(name = "created_at", updatable = false)
    LocalDateTime CREATED_AT;

    @Column(name = "updated_at")
    LocalDateTime UPDATE_AT;

    @PrePersist
    protected void onCreate() {
        CREATED_AT = LocalDateTime.now();
        UPDATE_AT = LocalDateTime.now();
        PROCESSED_AT = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        UPDATE_AT = LocalDateTime.now();
    }


}
