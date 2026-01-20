package org.project.floodalert.floodprocessor.model;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_reports")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserReport {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    String ID;

    @Column(name = "report_id", nullable = false, unique = true, length = 50)
    String REPORT_ID;

    @Column(name = "user_id", nullable = false)
    UUID USER_ID;

    @Column(name = "flood_event_id")
    UUID FLOOD_EVENT_ID;

    @Column(name = "description", columnDefinition = "TEXT")
    String DESCRIPTION;

    @Type(StringArrayType.class)
    @Column(name = "image_urls", columnDefinition = "text[]")
    String[] IMAGE_URLS;

    @Column(name = "severity_level", length = 20)
    String SEVERITY_LEVEL;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal LAT;

    @Column(name = "lon", nullable = false, precision = 11, scale = 8)
    BigDecimal LON;

    @Column(name = "status", length = 20)
    String STATUS = "PENDING";

    @Column(name = "reviewed_by")
    UUID REVIEWED_BY;

    @Column(name = "reviewed_at")
    LocalDateTime REVIEWED_AT;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String REJECT_REASON;

    @Column(name = "created_at", updatable = false)
    LocalDateTime CREATED_AT;

    @PrePersist
    protected void onCreate() {
        CREATED_AT = LocalDateTime.now();
    }
}
