package org.project.floodalert.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.LocalDateTime;

/**
 * DTO cho admin dashboard alerts
 * Chứa thông tin đã được enrich và format sẵn cho UI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAlertDTO {
    
    /**
     * ID của event
     */
    String eventId;
    
    /**
     * Loại event: CREATED, ESCALATED, DE_ESCALATED, RESOLVED, UPDATED
     */
    String type;
    
    /**
     * Mức nước hiện tại (cm)
     */
    Double waterLevel;
    
    /**
     * Mức độ nghiêm trọng: LOW, MEDIUM, HIGH, CRITICAL
     */
    String severityLevel;
    
    /**
     * Tên địa điểm
     */
    String location;
    
    /**
     * Vĩ độ
     */
    Double lat;
    
    /**
     * Kinh độ
     */
    Double lon;
    
    /**
     * Thời gian xảy ra event
     */
    LocalDateTime timestamp;
    
    /**
     * Message đã được format sẵn cho admin (tiếng Việt, có emoji)
     * VD: "🔴 Mức độ ngập gia tăng tại Quận 1 - Hiện tại: Nguy hiểm"
     */
    String alertMessage;
    
    /**
     * Mức độ cảnh báo cho UI: CRITICAL, DANGER, WARNING, SUCCESS, INFO
     * Frontend có thể dùng để chọn màu sắc, icon
     */
    String alertLevel;

    /**
     * Nguồn tạo ra flood event: SENSOR | USER_REPORT
     */
    String source;
}
