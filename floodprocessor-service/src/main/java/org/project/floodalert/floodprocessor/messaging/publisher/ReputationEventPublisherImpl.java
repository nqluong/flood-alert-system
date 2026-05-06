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

        String userIdKey = event.getUserId().toString();

        kafkaTemplate.send(reputationTopic, userIdKey, event);

        log.debug("[REPUTATION-PUBLISHER] Đã đặt lịch gửi: userId={}, reason={}, points={}",
                event.getUserId(), event.getReason(), event.getPoints());
    }
}
