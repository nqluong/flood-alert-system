package org.project.floodalert.notification.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationContext {
    UUID userId;

    @Builder.Default
    boolean isNearActive = false;

    Double activeDistance;
    
    /**
     * Danh sách các zone tĩnh (static locations) bị ảnh hưởng
     * Ví dụ: ["Nhà riêng", "Công ty"]
     */
    @Builder.Default
    List<String> affectedZones = new ArrayList<>();
    
    /**
     * Khoảng cách gần nhất từ static location đến điểm ngập (meters)
     */
    Double staticDistance;
    
    /**
     * Kiểm tra xem user có bị ảnh hưởng không (active hoặc static)
     */
    public boolean isAffected() {
        return isNearActive || !affectedZones.isEmpty();
    }
    
    /**
     * Kiểm tra xem có dính cả 2 loại location không
     */
    public boolean isBothAffected() {
        return isNearActive && !affectedZones.isEmpty();
    }
}
