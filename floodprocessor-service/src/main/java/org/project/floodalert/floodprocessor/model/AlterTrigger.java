package org.project.floodalert.floodprocessor.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alter_triggers")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlterTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID ID;

    @Column(name = "trigger_id", nullable = false, unique = true, length = 50)
    String TRIGGER_ID;

    @Column(name = "flood_event_id")
    UUID FLOOD_EVENT_ID;

    @Column(name = "trigger_type", nullable = false, length = 50)
    String TRIGGER_TYPE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_condition", columnDefinition = "jsonb")
    String TRIGGER_CONDITION;

    @Column(name = "affected_users_count")
    Integer AFFECTED_USERS_COUNT = 0;

    @Column(name = "search_radius_meters")
    Integer SEARCH_RADIUS_METERS;

    @Column(name = "search_center_lat", precision = 10, scale = 8)
    BigDecimal SEARCH_CENTER_LAT;

    @Column(name = "search_center_lon", precision = 11, scale = 8)
    BigDecimal SEARCH_CENTER_LON;

    @Column(name = "status", length = 20)
    String STATUS = "TRIGGERED";

    @Column(name = "triggered_at")
    LocalDateTime TRIGGERED_AT;

    @Column(name = "completed_at")
    LocalDateTime COMPLETED_AT;

    @PrePersist
    protected void onCreate() {
        TRIGGERED_AT = LocalDateTime.now();
    }
}
