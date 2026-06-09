package org.project.floodalert.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FcmTokenEvent;
import org.project.floodalert.notification.service.DeviceTokenService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class FcmTokenEventConsumer {

    private final DeviceTokenService deviceTokenService;

    @KafkaListener(
            topics = "${app.kafka.topic.fcm-token}",
            groupId = "${app.kafka.consumer.fcm-token.group-id}",
            containerFactory = "fcmTokenKafkaListenerContainerFactory"
    )
    public void consumeFcmTokenEvent(FcmTokenEvent event, Acknowledgment ack) {
        try {
            log.info("[FcmTokenEventConsumer] Nhận FCM token event - userId: {}, eventType: {}, isActive: {}", 
                    event.getUserId(), event.getEventType(), event.getIsActive());

            switch (event.getEventType()) {
                case "UPSERT":
                    log.info("[FcmTokenEventConsumer] Xử lý UPSERT event cho userId: {}", event.getUserId());
                    deviceTokenService.handleFcmTokenEvent(
                            event.getUserId(), 
                            event.getToken(), 
                            event.getIsActive()
                    );
                    break;

                case "DEACTIVATE":
                    log.info("[FcmTokenEventConsumer] Xử lý DEACTIVATE event cho userId: {}", event.getUserId());
                    deviceTokenService.handleFcmTokenEvent(
                            event.getUserId(), 
                            event.getToken(), 
                            false
                    );
                    break;

                case "DEACTIVATE_ALL":
                    log.info("[FcmTokenEventConsumer] Xử lý DEACTIVATE_ALL event cho userId: {}", event.getUserId());
                    deviceTokenService.handleFcmTokenEvent(
                            event.getUserId(), 
                            event.getToken(), 
                            false
                    );
                    break;

                case "CLEANUP":
                    log.info("[FcmTokenEventConsumer] Xử lý CLEANUP event cho userId: {}", event.getUserId());
                    deviceTokenService.handleFcmTokenEvent(
                            event.getUserId(), 
                            event.getToken(), 
                            false
                    );
                    break;

                default:
                    log.warn("[FcmTokenEventConsumer] Unknown event type: {} cho userId: {}", 
                            event.getEventType(), event.getUserId());
            }

            ack.acknowledge();
            log.info("[FcmTokenEventConsumer] Đã xử lý thành công FCM token event cho userId: {}", 
                    event.getUserId());

        } catch (Exception e) {
            log.error("[FcmTokenEventConsumer] Lỗi khi xử lý FCM token event - userId: {}, eventType: {}",
                    event.getUserId(), event.getEventType(), e);
            throw e;
        }
    }
}
