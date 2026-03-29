package org.project.floodalert.floodprocessor.service.gamification;

import org.project.floodalert.floodprocessor.dto.event.ReputationUpdateEvent;

public interface ReputationEventPublisher {

    /**
     * Publish reputation update event ra Kafka topic.
     * Sử dụng userId làm message key để đảm bảo ordering per user.
     *
     * @param event sự kiện reputation cần publish
     */
    void publish(ReputationUpdateEvent event);
}
