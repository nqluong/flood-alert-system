package org.project.floodalert.floodprocessor.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.config.SystemConfigService;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.messaging.publisher.LifecycleEventPublisher;
import org.project.floodalert.floodprocessor.tracker.SensorTelemetryTracker;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZombieFloodSweeperJob {

    private final SensorTelemetryTracker telemetryTracker;
    private final SystemConfigService configService;
    private final FloodEventRepository floodEventRepository;
    private final LifecycleEventPublisher lifecycleEventPublisher;

    @Scheduled(fixedDelay = 180000)
    public void sweepZombieFloodEvents() {
        long timeout = configService.getSensorTimeoutMs();
        long currentTime = System.currentTimeMillis();
        log.debug("Bắt đầu quét các sự kiện ngập zombie với timeout {} ms", timeout);

        List<FloodEvent> ongoingEvents = floodEventRepository.findActiveIotEvents();
        for (FloodEvent event : ongoingEvents) {
            String sensorId = event.getSourceId();
            Long lastSeen = telemetryTracker.getLastSeen(sensorId);

            if (lastSeen == null || (currentTime - lastSeen) > timeout) {
                event.setStatus("EXPIRED");
                floodEventRepository.save(event);

                log.debug("Sự kiện ngập ID {} đã được đóng do sensor {} không gửi tín hiệu trong {} ms.",
                        event.getId(), sensorId, timeout);

                FloodLifecycleEvent lifecycleEvent = FloodLifecycleEvent.builder()
                        .eventId(event.getEventId())
                        .type(LifecycleEventType.RESOLVED)
                        .location(event.getLocationDescription())
                        .lat(event.getLat().doubleValue())
                        .lon(event.getLon().doubleValue())
                        .waterLevel(null)
                        .severityLevel("SAFE")
                        .build();
                lifecycleEventPublisher.publish(lifecycleEvent);
            }
        }
    }
}
