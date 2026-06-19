package org.project.floodalert.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Internal DTO để xử lý flood events
 * Được convert từ FloodLifecycleEvent (từ Kafka)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloodEventDTO {
    
    /**
     * ID của sự kiện ngập lụt
     */
    String eventId;
    
    /**
     * Vĩ độ điểm ngập
     */
    Double lat;
    
    /**
     * Kinh độ điểm ngập
     */
    Double lon;
    
    /**
     * Bán kính ảnh hưởng (meters)
     */
    @Builder.Default
    Double radiusMeters = 5000.0;
    
    /**
     * Mức độ nghiêm trọng
     */
    String severityLevel;

    /**
     * Mức độ nghiêm trọng TRƯỚC thay đổi (dùng cho thông báo giảm/tăng cấp).
     * Null nếu không xác định.
     */
    String previousSeverityLevel;
    
    /**
     * Mức nước (cm)
     */
    Double waterLevel;
    
    /**
     * Địa điểm mô tả
     */
    String location;
    
    /**
     * Thời gian xảy ra
     */
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
}
