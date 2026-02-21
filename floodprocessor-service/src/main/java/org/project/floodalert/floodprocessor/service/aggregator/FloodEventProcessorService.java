package org.project.floodalert.floodprocessor.service.aggregator;

import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;

import java.util.List;

/**
 * Orchestrator của Module 5 (Event Aggregator).
 * Điều phối toàn bộ luồng xử lý cho một bản ghi sensor:
 *   1. Gọi FloodEventDbService để xử lý DB và back-linking
 *   2. Gọi FloodGeoCacheService để đồng bộ Redis Cache
 *   3. Gọi LifecycleEventPublisher để bắn sự kiện vòng đời (nếu cần)
 */
public interface FloodEventProcessorService {

    /**
     * Xử lý một bản ghi sensor đã được enrich và đánh giá trạng thái.
     *
     * @param data bản ghi sensor từ Kafka topic flood-processed-events
     */
    void process(ProcessedSensorData data);

    /**
     * Xử lý một batch các bản ghi sensor.
     * Từng bản ghi sẽ được xử lý độc lập; lỗi đơn lẻ không làm dừng cả batch.
     *
     * @param dataList danh sách bản ghi sensor từ Kafka
     */
    void processBatch(List<ProcessedSensorData> dataList);
}
