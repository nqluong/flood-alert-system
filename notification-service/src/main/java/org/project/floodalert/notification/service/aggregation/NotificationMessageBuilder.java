package org.project.floodalert.notification.service.aggregation;

import org.project.floodalert.notification.dto.event.FloodEventDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {

    public String title(FloodEventDTO event) {
        String location = resolveLocation(event);
        if (event.getWaterLevel() != null) {
            return String.format("Có điểm ngập lụt tại %s khoảng %.0fcm", location, event.getWaterLevel());
        }
        return String.format("Có điểm ngập lụt tại %s", location);
    }

    public String body(FloodEventDTO event) {
        String severityVi = translateSeverity(event.getSeverityLevel());
        String location = resolveLocation(event);

        if (event.getWaterLevel() != null) {
            return String.format(
                    "Tại %s đang có ngập lụt, mực nước khoảng %.0fcm (mức độ: %s). Hãy chú ý an toàn khi di chuyển!",
                    location, event.getWaterLevel(), severityVi);
        }
        return String.format(
                "Tại %s đang có ngập lụt (mức độ: %s). Hãy chú ý an toàn khi di chuyển!",
                location, severityVi);
    }

    public String resolvedTitle() {
        return "Khu vực đã hết ngập";
    }

    public String resolvedBody(FloodEventDTO event) {
        return String.format(
                "Tin vui: Điểm ngập tại %s đã hết ngập. Bạn có thể di chuyển bình thường.",
                resolveLocation(event));
    }

    private String resolveLocation(FloodEventDTO event) {
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            return event.getLocation();
        }
        if (event.getLat() != null && event.getLon() != null) {
            return String.format("(%.4f, %.4f)", event.getLat(), event.getLon());
        }
        return "vị trí chưa xác định";
    }

    public String translateSeverity(String severityLevel) {
        if (severityLevel == null) return "Không xác định";
        // Gom mọi sơ đồ severity (sensor: SAFE/WARNING/DANGER, báo cáo/FE: LOW/MEDIUM/HIGH)
        // về đúng 3 mức ngập hiển thị trên FE.
        return switch (severityLevel.toUpperCase()) {
            case "HIGH", "DANGER", "CRITICAL"   -> "Ngập cao";
            case "MEDIUM", "WARNING"            -> "Ngập trung bình";
            case "LOW"                          -> "Ngập nhẹ";
            case "NONE", "SAFE"                 -> "Không ngập";
            default                             -> "Không xác định";
        };
    }
}
