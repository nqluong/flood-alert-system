package org.project.floodalert.floodcore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "core_active_floods")
public class CoreActiveFlood {

    @Id
    @Column(name = "event_id")
    String eventId;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    BigDecimal lat;

    @Column(name = "lon", nullable = false, precision = 10, scale = 8)
    BigDecimal lon;

    @Column(name = "location_description")
    String locationDescription;

    @Column(name = "water_level", nullable = false, precision = 5, scale = 2)
    BigDecimal waterLevel;

    @Column(name = "severity_level")
    String severityLevel;

    @Column(name = "status")
    String status;

    @Column(name = "source", length = 20)
    String source;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PreUpdate
    protected void  preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
