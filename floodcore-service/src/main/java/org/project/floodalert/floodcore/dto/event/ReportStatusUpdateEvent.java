package org.project.floodalert.floodcore.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Event nhận từ flood-processor để cập nhật status của UserReport
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportStatusUpdateEvent {
    
    String reportId;
    UUID userId;
    String status;  // APPROVED, REJECTED, PENDING
    String eventId;
    String rejectReason;
    Double score;
    Double aiScore;
    Double spatialScore;
    Double reputationScore;
}
