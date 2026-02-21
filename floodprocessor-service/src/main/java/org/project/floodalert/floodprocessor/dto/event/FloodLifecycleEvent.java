package org.project.floodalert.floodprocessor.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;

import java.time.LocalDateTime;

/**
 * DTO đại diện cho một sự kiện vòng đời của Flood Event.
 * Được bắn ra Kafka topic "flood-lifecycle-events" để Admin và Notification service tiêu thụ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloodLifecycleEvent {

    /** ID định danh duy nhất của flood event liên quan */
    String eventId;

    /** Loại vòng đời: CREATED, ESCALATED, RESOLVED */
    LifecycleEventType type;

    /** Mức nước tại thời điểm sự kiện vòng đời xảy ra (đơn vị: cm) */
    Double waterLevel;

    /** Mức độ cảnh báo dạng chuỗi: SAFE, WARNING, DANGER */
    String severityLevel;

    /** Mô tả địa điểm ngập */
    String location;

    /** Vĩ độ của địa điểm ngập */
    Double lat;

    /** Kinh độ của địa điểm ngập */
    Double lon;

    /** Thời điểm sự kiện vòng đời này được tạo ra */
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
}
