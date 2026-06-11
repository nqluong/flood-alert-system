package org.project.floodalert.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FloodEventDTO;
import org.project.floodalert.notification.model.NotificationContext;
import org.project.floodalert.notification.service.aggregation.ActiveLocationScanner;
import org.project.floodalert.notification.service.aggregation.NotificationMessageBuilder;
import org.project.floodalert.notification.service.aggregation.StaticLocationScanner;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAggregationService {

    private final ActiveLocationScanner activeLocationScanner;
    private final StaticLocationScanner staticLocationScanner;
    private final NotificationMessageBuilder messageBuilder;

    public Map<UUID, NotificationContext> aggregateNotificationContexts(FloodEventDTO event, boolean includeActive) {
        log.debug("[Aggregator] event={}, includeActive={}", event.getEventId(), includeActive);

        try {
            if (!includeActive) {
                return staticLocationScanner.scan(event);
            }

            CompletableFuture<Map<UUID, NotificationContext>> activeFuture =
                    CompletableFuture.supplyAsync(() -> activeLocationScanner.scan(event));
            CompletableFuture<Map<UUID, NotificationContext>> staticFuture =
                    CompletableFuture.supplyAsync(() -> staticLocationScanner.scan(event));

            CompletableFuture.allOf(activeFuture, staticFuture).join();

            Map<UUID, NotificationContext> merged = mergeContexts(activeFuture.get(), staticFuture.get());
            log.debug("[Aggregator] eventId={} → {} unique users", event.getEventId(), merged.size());
            return merged;

        } catch (Exception e) {
            log.error("[Aggregator] Lỗi khi aggregate cho event {}", event.getEventId(), e);
            return Collections.emptyMap();
        }
    }

    private Map<UUID, NotificationContext> mergeContexts(Map<UUID, NotificationContext> active,
                                                         Map<UUID, NotificationContext> staticMap) {
        Map<UUID, NotificationContext> merged = new HashMap<>(active);
        staticMap.forEach((userId, staticCtx) -> merged.merge(userId, staticCtx, (existing, incoming) -> {
            existing.getAffectedZones().addAll(incoming.getAffectedZones());
            existing.setStaticDistance(incoming.getStaticDistance());
            return existing;
        }));
        return merged;
    }

    public String generateNotificationTitle(FloodEventDTO event) {
        return messageBuilder.title(event);
    }

    public String generateNotificationBody(FloodEventDTO event) {
        return messageBuilder.body(event);
    }

    public String generateResolvedTitle() {
        return messageBuilder.resolvedTitle();
    }

    public String generateResolvedBody(FloodEventDTO event) {
        return messageBuilder.resolvedBody(event);
    }
}
