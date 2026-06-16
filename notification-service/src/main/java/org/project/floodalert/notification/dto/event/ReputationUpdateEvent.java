package org.project.floodalert.notification.dto.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReputationUpdateEvent {

    UUID userId;

    /** Flood Event ID liên quan */
    String eventId;

    /** Lý do thay đổi (AUTO_REJECTED, CLUSTER_APPROVED, ACTIVE_CONFIRMATION, ADMIN_APPROVED, ADMIN_REJECTED) */
    String reason;

    /** Điểm thay đổi — dương = reward, âm = penalty */
    Integer points;

    /** Report ID gốc gây ra sự thay đổi */
    String reportId;

    Double lat;

    Double lon;

    String address;

    LocalDateTime timestamp;
}
