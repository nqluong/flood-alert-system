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
    UUID id;

    @Column(name = "trigger_id", nullable = false, unique = true, length = 50)
    String triggerId;

    @Column(name = "flood_event_id")
    UUID floodEventId;

    @Column(name = "trigger_type", nullable = false, length = 50)
    String triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_condition", columnDefinition = "jsonb")
    String triggerCondition;

    @Column(name = "affected_users_count")
    Integer affectedUserCount = 0;

    @Column(name = "search_radius_meters")
    Integer searchRadiusMeters;

    @Column(name = "search_center_lat", precision = 10, scale = 8)
    BigDecimal searchCenterLat;

    @Column(name = "search_center_lon", precision = 11, scale = 8)
    BigDecimal searchCenterLon;

    @Column(name = "status", length = 20)
    String status = "TRIGGERED";

    @Column(name = "triggered_at")
    LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        triggeredAt = LocalDateTime.now();
    }
}
