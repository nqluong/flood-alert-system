package org.project.floodalert.auth.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReputationUpdateEvent {

    UUID userId;

    /** Flood Event ID liên quan */
    String eventId;

    /** Lý do thay đổi (AUTO_REJECTED, CLUSTER_APPROVED, ...) */
    String reason;

    /** Điểm thay đổi — dương = reward, âm = penalty */
    Integer points;

    /** Report ID gốc gây ra sự thay đổi */
    String reportId;

    LocalDateTime timestamp;
}
