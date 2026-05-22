package org.project.floodalert.notification.service.aggregation;

import org.project.floodalert.notification.dto.event.FloodEventDTO;
import org.project.floodalert.notification.model.NotificationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationMessageBuilder {

    public String title(NotificationContext context) {
        if (context.isBothAffected()) return "Cảnh báo ngập lụt khẩn cấp";
        if (context.isNearActive())   return "Ngập lụt gần vị trí của bạn";
        return "Cảnh báo ngập lụt khu vực";
    }

    public String body(NotificationContext context, FloodEventDTO event) {
        String severityVi = translateSeverity(event.getSeverityLevel());

        if (context.isBothAffected()) {
            return String.format(
                    "Khu vực bạn đang di chuyển VÀ gần %s đang có ngập lụt (mức độ: %s). Hãy chú ý an toàn!",
                    formatZones(context.getAffectedZones()), severityVi);
        }
        if (context.isNearActive()) {
            return String.format(
                    "Có điểm ngập lụt cách vị trí hiện tại của bạn khoảng %.0fm (mức độ: %s). Chú ý hướng di chuyển!",
                    context.getActiveDistance(), severityVi);
        }

        List<String> zones = context.getAffectedZones();
        if (zones.size() == 1) {
            return String.format(
                    "Cảnh báo: Khu vực quanh %s của bạn vừa xuất hiện điểm ngập (mức độ: %s).",
                    zones.get(0), severityVi);
        }
        return String.format(
                "Cảnh báo: %d địa điểm bạn đã lưu (%s) có điểm ngập lân cận (mức độ: %s).",
                zones.size(), formatZones(zones), severityVi);
    }

    private String formatZones(List<String> zones) {
        if (zones == null || zones.isEmpty()) return "";
        if (zones.size() == 1) return zones.get(0);
        return String.join(", ", zones.subList(0, zones.size() - 1))
                + " và " + zones.get(zones.size() - 1);
    }

    public String resolvedTitle() {
        return "Khu vực đã hết ngập";
    }

    public String resolvedBody(NotificationContext context, FloodEventDTO event) {
        String zones = context.getAffectedZones().isEmpty()
                ? (event.getLocation() != null ? event.getLocation() : "khu vực bạn quan tâm")
                : String.join(", ", context.getAffectedZones());
        return String.format(
                "Tin vui: Khu vực quanh %s đã hết ngập. Bạn có thể di chuyển bình thường.",
                zones);
    }

    public String translateSeverity(String severityLevel) {
        if (severityLevel == null) return "Không xác định";
        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL"         -> "Cực kỳ nguy hiểm";
            case "DANGER"           -> "Nguy hiểm";
            case "WARNING"          -> "Cảnh báo";
            case "MEDIUM"           -> "Trung bình";
            case "LOW"              -> "Nhẹ";
            case "NONE", "SAFE"     -> "Không ngập";
            default                 -> severityLevel;
        };
    }
}
