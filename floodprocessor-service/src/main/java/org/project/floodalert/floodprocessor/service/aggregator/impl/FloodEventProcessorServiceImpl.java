package org.project.floodalert.floodprocessor.service.aggregator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.messaging.publisher.LifecycleEventPublisher;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService.FloodEventDbResult;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventProcessorService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrator của Module 5 (Event Aggregator).
 * Điều phối tuần tự 3 bước cho mỗi bản ghi sensor:
 * <ol>
 *   <li>Xử lý DB (kịch bản A/B/C) và back-link IoTReading</li>
 *   <li>Publish Lifecycle Event ra Kafka (nếu cần)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FloodEventProcessorServiceImpl implements FloodEventProcessorService {

    private final FloodEventDbService floodEventDbService;
//    private final FloodGeoCacheService floodGeoCacheService;
    private final LifecycleEventPublisher lifecycleEventPublisher;


    /**
     * {@inheritDoc}
     * Xử lý một bản ghi sensor đơn lẻ qua đầy đủ 3 bước.
     */
    @Override
    public void process(ProcessedSensorData data) {
        if (data == null || data.getSensorId() == null) {
            log.warn("Dữ liệu sensor null hoặc thiếu sensorId, bỏ qua xử lý aggregator");
            return;
        }

        try {
            // Bước 1 & 2: Xử lý DB (kịch bản A/B/C) + back-link IoTReading
            FloodEventDbResult dbResult = floodEventDbService.processAndSave(data);

            if (dbResult.floodEvent() == null) {
                // Không có hành động (VD: status SAFE nhưng không có event active)
                log.debug("Không có flood event nào được xử lý cho sensor [{}]", data.getSensorId());
                return;
            }

            FloodEvent floodEvent = dbResult.floodEvent();

            // Đồng bộ Redis Cache
//            syncRedisCache(data.getStatus(), floodEvent);

            // Publish Lifecycle Event (nếu cần)
            if (dbResult.shouldPublish()) {
                FloodLifecycleEvent lifecycleEvent =
                        buildLifecycleEvent(floodEvent, dbResult);
                lifecycleEventPublisher.publish(lifecycleEvent);
            }

            log.debug("Hoàn thành xử lý aggregator cho sensor [{}], sự kiện [{}], type [{}]",
                    data.getSensorId(), floodEvent.getEventId(), dbResult.lifecycleEventType());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý aggregator cho sensor [{}]: {}",
                    data.getSensorId(), e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * Xử lý từng bản ghi trong batch độc lập.
     * Lỗi của một bản ghi không làm dừng các bản ghi còn lại.
     */
    @Override
    public void processBatch(List<ProcessedSensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.debug("Batch rỗng, bỏ qua xử lý aggregator");
            return;
        }

        log.info("Bắt đầu xử lý aggregator batch: {} bản ghi", dataList.size());

        int successCount = 0;
        int failCount = 0;

        for (ProcessedSensorData data : dataList) {
            try {
                process(data);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Lỗi xử lý aggregator cho sensor [{}] trong batch: {}",
                        data != null ? data.getSensorId() : "null", e.getMessage());
            }
        }

        log.info("Hoàn thành aggregator batch: {}/{} thành công, {} lỗi",
                successCount, dataList.size(), failCount);
    }


    /**
     * Đồng bộ Redis Cache dựa trên trạng thái sensor mới nhất.
     * - DANGER/WARNING (kịch bản B hoặc C): thêm/cập nhật vào GEO set và Hash.
     * - SAFE (kịch bản A): xóa khỏi GEO set và Hash.
     */
//    private void syncRedisCache(FloodStatus newStatus, FloodEvent floodEvent) {
//        if (newStatus == FloodStatus.SAFE) {
//            // Kịch bản A – Nước rút: xóa khỏi cache
//            floodGeoCacheService.removeResolvedFloodEvent(floodEvent.getEventId());
//        } else {
//            // Kịch bản B hoặc C – Đang ngập: thêm/cập nhật cache
//            floodGeoCacheService.syncActiveFloodEvent(floodEvent);
//        }
//    }

    /**
     * Xây dựng {@link FloodLifecycleEvent} từ kết quả DB để publish ra Kafka.
     */
    private FloodLifecycleEvent buildLifecycleEvent(FloodEvent floodEvent,
                                                     FloodEventDbResult dbResult) {
        return FloodLifecycleEvent.builder()
                .eventId(floodEvent.getEventId())
                .type(dbResult.lifecycleEventType())
                .waterLevel(floodEvent.getWaterLevel() != null
                        ? floodEvent.getWaterLevel().doubleValue() : null)
                .severityLevel(floodEvent.getSeverityLevel())
                .location(floodEvent.getLocationDescription())
                .lat(floodEvent.getLat() != null ? floodEvent.getLat().doubleValue() : null)
                .lon(floodEvent.getLon() != null ? floodEvent.getLon().doubleValue() : null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
