package org.project.floodalert.floodprocessor.messaging.publisher;

import org.project.floodalert.floodprocessor.dto.event.ReputationUpdateEvent;

public interface ReputationEventPublisher {

    void publish(ReputationUpdateEvent event);
}
