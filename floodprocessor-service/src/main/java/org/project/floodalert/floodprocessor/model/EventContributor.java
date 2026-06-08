package org.project.floodalert.floodprocessor.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "event_contributors")
public class EventContributor {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "flood_event_id", nullable = false)
    UUID floodEventId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "role")
    String role;

    @Column(name = "report_id", length = 50)
    String reportId;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
