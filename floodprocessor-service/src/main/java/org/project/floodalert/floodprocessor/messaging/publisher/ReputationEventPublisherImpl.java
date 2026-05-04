package org.project.floodalert.floodprocessor.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.ReputationUpdateEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReputationEventPublisherImpl implements ReputationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.reputation:reputation-update-events}")
    private String reputationTopic;

    @Override
    public void publish(ReputationUpdateEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("[REPUTATION-PUBLISHER] Event hoặc userId bị null, bỏ qua publish");
            return;
        }

        // Gửi Kafka với userId làm key để đảm bảo ordering theo user
        String userIdKey = event.getUserId().toString();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(reputationTopic, userIdKey, event);

        // Callback bất đồng bộ để log kết quả
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[REPUTATION-PUBLISHER] Gửi thất bại cho user [{}], event [{}], reason [{}]: {}",
                        event.getUserId(), event.getEventId(), event.getReason(), ex.getMessage());
            } else {
                var meta = result.getRecordMetadata();
                log.info("[REPUTATION-PUBLISHER] Gửi thành công: userId={}, eventId={}, reason={}, " +
                                "points={}, topic={}, partition={}, offset={}",
                        event.getUserId(), event.getEventId(), event.getReason(), event.getPoints(),
                        meta.topic(), meta.partition(), meta.offset());
            }
        });

        log.debug("[REPUTATION-PUBLISHER] Đã đặt lịch gửi: userId={}, reason={}, points={}",
                event.getUserId(), event.getReason(), event.getPoints());
    }
}
