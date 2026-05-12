package org.project.floodalert.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.UserLocationEvent;
import org.project.floodalert.notification.service.UserLocationSyncService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLocationEventConsumer {

    private final UserLocationSyncService userLocationSyncService;

    @KafkaListener(
            topics = "${app.kafka.topic.user-location}",
            groupId = "${app.kafka.consumer.user-location.group-id}",
            containerFactory = "userLocationKafkaListenerContainerFactory"
    )
    public void consumeUserLocationEvent(UserLocationEvent event, Acknowledgment ack) {
        try {
            log.debug("[UserLocationEventConsumer] Nhận user location event - userId: {}, eventType: {}, location: ({}, {})",
                    event.getUserId(), event.getEventType(), event.getLatitude(), event.getLongitude());

            switch (event.getEventType()) {
                case "UPSERT":
                    log.debug("[UserLocationEventConsumer] Đồng bộ vị trí user {} vào Redis Geo", event.getUserId());
                    userLocationSyncService.syncUserLocationToRedis(
                            event.getUserId(),
                            event.getLatitude(),
                            event.getLongitude()
                    );
                    break;

                case "DELETE":
                    log.debug("[UserLocationEventConsumer] Xóa vị trí user {} khỏi Redis Geo", event.getUserId());
                    userLocationSyncService.removeUserLocationFromRedis(event.getUserId());
                    break;

                default:
                    log.warn("[UserLocationEventConsumer] Unknown event type: {} cho userId: {}",
                            event.getEventType(), event.getUserId());
            }

            ack.acknowledge();
            log.info("[UserLocationEventConsumer] Đã xử lý thành công user location event cho userId: {}",
                    event.getUserId());

        } catch (Exception e) {
            log.error("[UserLocationEventConsumer] Lỗi khi xử lý user location event - userId: {}, eventType: {}",
                    event.getUserId(), event.getEventType(), e);
            // Không acknowledge để Kafka retry
        }
    }
}
