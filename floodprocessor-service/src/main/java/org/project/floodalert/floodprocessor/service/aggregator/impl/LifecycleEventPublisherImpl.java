package org.project.floodalert.floodprocessor.service.aggregator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.service.aggregator.LifecycleEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Triển khai {@link LifecycleEventPublisher}.
 * Serialize FloodLifecycleEvent thành JSON và bắn ra Kafka topic "flood-lifecycle-events".
 * Sử dụng eventId làm Kafka message key để đảm bảo ordering theo từng sự kiện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleEventPublisherImpl implements LifecycleEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Topic nhận lifecycle event, cấu hình qua application.yml */
    @Value("${app.kafka.topic.lifecycle:flood-lifecycle-events}")
    private String lifecycleTopic;


    /**
     * {@inheritDoc}
     * Gửi FloodLifecycleEvent bất đồng bộ (async) lên Kafka.
     * Kết quả được log qua callback.
     */
    @Override
    public void publish(FloodLifecycleEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Lifecycle event hoặc eventId bị null, bỏ qua publish");
            return;
        }

        // Gửi Kafka với eventId làm key để đảm bảo ordering theo event
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(lifecycleTopic, event.getEventId(), event);

        // Callback bất đồng bộ để log kết quả
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Gửi Lifecycle Event thất bại cho sự kiện [{}] – type [{}]: {}",
                        event.getEventId(), event.getType(), ex.getMessage());
            } else {
                var meta = result.getRecordMetadata();
                log.info("Lifecycle Event đã gửi thành công: sự kiện=[{}], type=[{}], " +
                                "topic={}, partition={}, offset={}",
                        event.getEventId(), event.getType(),
                        meta.topic(), meta.partition(), meta.offset());
            }
        });

        log.debug("Đã đặt lịch gửi Lifecycle Event [{}] type [{}] lên topic [{}]",
                event.getEventId(), event.getType(), lifecycleTopic);
    }
}
