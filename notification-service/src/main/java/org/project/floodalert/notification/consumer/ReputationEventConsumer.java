package org.project.floodalert.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.ReputationUpdateEvent;
import org.project.floodalert.notification.enums.NotificationChannel;
import org.project.floodalert.notification.enums.NotificationPriority;
import org.project.floodalert.notification.enums.NotificationStatus;
import org.project.floodalert.notification.model.Notification;
import org.project.floodalert.notification.service.FcmDispatchService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReputationEventConsumer {

    private final FcmDispatchService fcmDispatchService;

    private static final int MAX_RETRIES = 3;

    private static final String NOTIFICATION_TYPE_REPORT_APPROVED = "REPORT_APPROVED";
    private static final String NOTIFICATION_TYPE_REPORT_REJECTED = "REPORT_REJECTED";

    private static final Set<String> APPROVED_REASONS =
            Set.of("CLUSTER_APPROVED", "ADMIN_APPROVED", "ACTIVE_CONFIRMATION");
    private static final Set<String> REJECTED_REASONS =
            Set.of("AUTO_REJECTED", "ADMIN_REJECTED");

    @KafkaListener(
            topics = "${app.kafka.topic.reputation:reputation-update-events}",
            groupId = "${app.kafka.consumer.reputation.group-id:notification-reputation-group}",
            containerFactory = "reputationKafkaListenerContainerFactory")
    @Transactional
    public void consumeReputationUpdateEvent(ReputationUpdateEvent event, Acknowledgment ack) {
        try {
            if (event == null || event.getUserId() == null || event.getReason() == null) {
                log.warn("[ReputationConsumer] Event thiếu userId/reason — bỏ qua");
                ack.acknowledge();
                return;
            }

            boolean approved = APPROVED_REASONS.contains(event.getReason());
            if (!approved && !REJECTED_REASONS.contains(event.getReason())) {
                log.warn("[ReputationConsumer] Reason không xác định: {} — bỏ qua. reportId={}",
                        event.getReason(), event.getReportId());
                ack.acknowledge();
                return;
            }

            Notification notification = buildNotification(event, approved);
            int successCount = fcmDispatchService.sendBatch(List.of(notification));

            log.info("[ReputationConsumer] Đã gửi thông báo kết quả báo cáo: userId={}, reason={}, points={}, pushSuccess={}",
                    event.getUserId(), event.getReason(), event.getPoints(), successCount);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[ReputationConsumer] Lỗi xử lý reputation event: userId={}, reportId={}",
                    event != null ? event.getUserId() : null,
                    event != null ? event.getReportId() : null, e);
        }
    }

    private Notification buildNotification(ReputationUpdateEvent event, boolean approved) {
        return Notification.builder()
                .userId(event.getUserId())
                .title(buildTitle(approved))
                .body(buildBody(event, approved))
                .notificationType(approved ? NOTIFICATION_TYPE_REPORT_APPROVED : NOTIFICATION_TYPE_REPORT_REJECTED)
                .priority(NotificationPriority.NORMAL)
                .data(buildData(event, approved))
                .channel(NotificationChannel.PUSH)
                .fcmToken(null)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetries(MAX_RETRIES)
                .build();
    }

    private String buildTitle(boolean approved) {
        return approved
                ? "Báo cáo ngập của bạn đã được chấp nhận"
                : "Báo cáo ngập của bạn đã bị từ chối";
    }

    private String buildBody(ReputationUpdateEvent event, boolean approved) {
        int points = event.getPoints() != null ? event.getPoints() : 0;
        String reasonVi = translateReason(event.getReason());

        if (approved) {
            if (points > 0) {
                return String.format(
                        "Báo cáo ngập lụt của bạn đã được xác nhận (%s). Bạn được cộng %d điểm uy tín. Cảm ơn bạn đã đóng góp!",
                        reasonVi, points);
            }
            return String.format(
                    "Báo cáo ngập lụt của bạn đã được xác nhận (%s). Cảm ơn bạn đã đóng góp!", reasonVi);
        }

        if (points < 0) {
            return String.format(
                    "Báo cáo ngập lụt của bạn không được chấp nhận (%s). Bạn bị trừ %d điểm uy tín.",
                    reasonVi, Math.abs(points));
        }
        return String.format("Báo cáo ngập lụt của bạn không được chấp nhận (%s).", reasonVi);
    }

    private String translateReason(String reason) {
        return switch (reason) {
            case "CLUSTER_APPROVED" -> "được cộng đồng xác nhận";
            case "ADMIN_APPROVED" -> "được quản trị viên phê duyệt";
            case "ACTIVE_CONFIRMATION" -> "xác nhận thêm cho điểm ngập đang hoạt động";
            case "AUTO_REJECTED" -> "độ tin cậy không đủ";
            case "ADMIN_REJECTED" -> "quản trị viên từ chối";
            default -> reason;
        };
    }

    private Map<String, Object> buildData(ReputationUpdateEvent event, boolean approved) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportId", event.getReportId());
        data.put("eventId", event.getEventId());
        data.put("reason", event.getReason());
        data.put("points", event.getPoints());
        data.put("reportStatus", approved ? "APPROVED" : "REJECTED");
        if (event.getLat() != null) {
            data.put("lat", event.getLat());
        }
        if (event.getLon() != null) {
            data.put("lon", event.getLon());
        }
        if (event.getAddress() != null && !event.getAddress().isBlank()) {
            data.put("location", event.getAddress());
        }
        if (event.getTimestamp() != null) {
            data.put("timestamp", event.getTimestamp().toString());
        }
        return data;
    }
}
