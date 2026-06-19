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
import java.util.ArrayList;
import java.util.List;

/**
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
    private final LifecycleEventPublisher lifecycleEventPublisher;

    @Override
    public void processBatch(List<ProcessedSensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.debug("Batch rỗng, bỏ qua xử lý aggregator");
            return;
        }

        log.debug("Bắt đầu xử lý batch {} sensors với batch optimization", dataList.size());

        try {
            List<FloodEventDbResult> results = floodEventDbService.processAndSaveBatch(dataList);

            List<FloodLifecycleEvent> eventsToPublish = new ArrayList<>();

            for (int i = 0; i < results.size(); i++) {
                FloodEventDbResult result = results.get(i);
                ProcessedSensorData data = dataList.get(i);

                if (result.shouldPublish() && result.floodEvent() != null) {
                    eventsToPublish.add(buildLifecycleEvent(result.floodEvent(), result, data));
                }

                log.debug("Hoàn thành xử lý aggregator cho sensor [{}], sự kiện [{}], type [{}]",
                        data.getSensorId(),
                        result.floodEvent() != null ? result.floodEvent().getEventId() : "null",
                        result.lifecycleEventType());
            }

            if (!eventsToPublish.isEmpty()) {
                lifecycleEventPublisher.publishAll(eventsToPublish);
                log.debug("Batch publish: Gửi {} lifecycle events ra Kafka", eventsToPublish.size());
            }

            log.info("Hoàn thành xử lý batch: {}/{} sensors thành công", 
                    results.size(), dataList.size());

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi xử lý batch, fallback sang xử lý từng sensor: {}", 
                    e.getMessage(), e);
        }
    }

    private FloodLifecycleEvent buildLifecycleEvent(FloodEvent floodEvent,
                                                     FloodEventDbResult dbResult,
                                                     ProcessedSensorData data) {
        return FloodLifecycleEvent.builder()
                .eventId(floodEvent.getEventId())
                .type(dbResult.lifecycleEventType())
                // Dùng snapshot được capture tại thời điểm tạo result, tránh bị ghi đè bởi message kế tiếp trong batch
                .waterLevel(dbResult.waterLevelSnapshot())
                .severityLevel(dbResult.severityLevelSnapshot())
                // Severity trước thay đổi (để consumer hiển thị "giảm từ X về Y") — null nếu chưa có lịch sử
                .previousSeverityLevel(data.getPreviousStatus() != null ? data.getPreviousStatus().name() : null)
                .location(floodEvent.getLocationDescription())
                .lat(floodEvent.getLat())
                .lon(floodEvent.getLon())
                .source(floodEvent.getSource())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
