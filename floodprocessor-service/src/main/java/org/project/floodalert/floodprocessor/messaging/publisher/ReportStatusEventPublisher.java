package org.project.floodalert.floodprocessor.messaging.publisher;

import org.project.floodalert.floodprocessor.dto.event.ReportStatusUpdateEvent;

public interface ReportStatusEventPublisher {

    void publish(ReportStatusUpdateEvent event);
}
