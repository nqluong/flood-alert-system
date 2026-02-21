package org.project.floodalert.floodprocessor.service.aggregator;

import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;

/**
 * Service chịu trách nhiệm bắn Lifecycle Event ra Kafka topic "flood-lifecycle-events".
 * Admin service và Notification service sẽ lắng nghe topic này để xử lý tiếp.
 *
 * Chỉ bắn sự kiện trong 3 trường hợp:
 * 1. Kịch bản A (Nước rút) → type = RESOLVED
 * 2. Kịch bản B (Ngập mới) → type = CREATED
 * 3. Kịch bản C (Ngập kéo dài, mức độ tăng) → type = ESCALATED
 */
public interface LifecycleEventPublisher {

    /**
     * Serialize và bắn FloodLifecycleEvent ra Kafka topic lifecycle.
     * Sử dụng eventId làm Kafka message key để đảm bảo ordering theo event.
     *
     * @param event sự kiện vòng đời cần publish
     */
    void publish(FloodLifecycleEvent event);
}
