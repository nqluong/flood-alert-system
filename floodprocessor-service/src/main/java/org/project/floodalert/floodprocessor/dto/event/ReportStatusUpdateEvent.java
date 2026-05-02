package org.project.floodalert.floodprocessor.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Event để thông báo kết quả xử lý report về flood-core service
 * để cập nhật status của UserReport
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportStatusUpdateEvent {
    
    /**
     * ID của report (reportId string)
     */
    String reportId;
    
    /**
     * User ID người tạo report
     */
    UUID userId;
    
    /**
     * Status mới: APPROVED, REJECTED, PENDING
     */
    String status;
    
    /**
     * Event ID mà report này thuộc về (nếu APPROVED)
     */
    String eventId;
    
    /**
     * Lý do reject (nếu status = REJECTED)
     */
    String rejectReason;
    
    /**
     * Điểm số đạt được từ scoring engine
     */
    Double score;
}
