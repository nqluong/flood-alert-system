package org.project.floodalert.auth.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.project.floodalert.auth.dto.event.ReputationUpdateEvent;
import org.project.floodalert.auth.service.ReputationCacheService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReputationEventConsumer {

    private final ReputationCacheService reputationCacheService;

    @KafkaListener(
            topics = "${app.kafka.topic.reputation:reputation-update-events}",
            groupId = "${app.kafka.consumer.reputation.group-id:auth-service-reputation-consumer}",
            containerFactory = "reputationKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ReputationUpdateEvent> record) {
        ReputationUpdateEvent event = record.value();

        if (event == null || event.getUserId() == null || event.getPoints() == null) {
            log.warn("[REPUTATION-CONSUMER] Event không hợp lệ tại partition={}, offset={} — bỏ qua",
                    record.partition(), record.offset());
            return;
        }

        log.info("[REPUTATION-CONSUMER] Nhận event: userId={}, reason={}, points={}, reportId={}",
                event.getUserId(), event.getReason(), event.getPoints(), event.getReportId());

        try {
            reputationCacheService.updateAndCache(event.getUserId(), event.getPoints());
            log.debug("[REPUTATION-CONSUMER] Xử lý thành công: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[REPUTATION-CONSUMER] Lỗi khi cập nhật reputation: userId={}, reason={}, error={}",
                    event.getUserId(), event.getReason(), e.getMessage(), e);
            // Re-throw để Kafka retry theo cấu hình error handler mặc định
            throw e;
        }
    }
}
