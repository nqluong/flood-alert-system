package org.project.floodalert.floodcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.enums.LifecycleEventType;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreActiveFloodSyncService {

    private final CoreActiveFloodRepository coreActiveFloodRepository;
    private final FloodGeoCache floodGeoCache;

    /**
     * Nhận sự kiện vòng đời ngập lụt từ Kafka topic flood-lifecycle-events.
     *
     * @param event          payload sự kiện ngập lụt đã deserialize
     * @param acknowledgment dùng để commit offset thủ công sau khi xử lý xong
     */
    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.lifecycle}",
            groupId = "${app.kafka.consumer.core-sync.group-id:core-map-sync-group}",
            containerFactory = "coreKafkaListenerContainerFactory"
    )
    public void onFloodLifecycleEvent(FloodLifecycleEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            log.warn("[FloodListener] Nhận được sự kiện null, bỏ qua");
            acknowledgment.acknowledge();
            return;
        }

        try {
            if (LifecycleEventType.RESOLVED.equals(event.getType())) {
                handleResolved(event);
            } else {
                // CREATED hoặc ESCALATED
                handleUpsert(event);
            }

            log.info("[FloodListener] Xử lý thành công eventId={}, type={}",
                    event.getEventId(), event.getType());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("[FloodListener] Lỗi xử lý eventId={}: {}", event.getEventId(), e.getMessage(), e);
            // Vẫn acknowledge để tránh replay vô tận; có thể đẩy sang DLQ nếu cần
            acknowledgment.acknowledge();
        }
    }

    private void handleResolved(FloodLifecycleEvent event) {
        // Cập nhật / xóa DB
        if (coreActiveFloodRepository.existsById(event.getEventId())) {
            coreActiveFloodRepository.deleteById(event.getEventId());
            log.debug("[FloodListener] Đã xóa DB eventId={}", event.getEventId());
        } else {
            log.warn("[FloodListener] Không tìm thấy bản ghi DB để xóa, eventId={}", event.getEventId());
        }

        // Xóa Redis cache
        floodGeoCache.removeFloodCache(event.getEventId());
    }

    private void handleUpsert(FloodLifecycleEvent event) {
        // UPSERT vào PostgreSQL
        CoreActiveFlood flood = coreActiveFloodRepository
                .findById(event.getEventId())
                .orElseGet(() -> CoreActiveFlood.builder()
                        .eventId(event.getEventId())
                        .build());

        flood.setLat(event.getLat() != null ? BigDecimal.valueOf(event.getLat()) : null);
        flood.setLon(event.getLon() != null ? BigDecimal.valueOf(event.getLon()) : null);
        flood.setLocationDescription(event.getLocation());
        flood.setWaterLevel(event.getWaterLevel() != null ? BigDecimal.valueOf(event.getWaterLevel()) : null);
        flood.setSeverityLevel(event.getSeverityLevel());
        flood.setStatus("CONFIRMED");

        coreActiveFloodRepository.save(flood);
        log.debug("[FloodListener] Đã upsert DB eventId={}", event.getEventId());

        // Cập nhật Redis cache
        floodGeoCache.cacheActiveFlood(event);
    }
}
