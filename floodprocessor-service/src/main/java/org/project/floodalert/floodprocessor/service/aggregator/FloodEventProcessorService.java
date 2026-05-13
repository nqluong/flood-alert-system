package org.project.floodalert.floodprocessor.service.aggregator;

import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;

import java.util.List;

/*
 * Điều phối toàn bộ luồng xử lý cho một bản ghi sensor:
 *   Gọi FloodEventDbService để xử lý DB và back-linking
 *   Gọi FloodGeoCacheService để đồng bộ Redis Cache
 *   Gọi LifecycleEventPublisher để bắn sự kiện vòng đời
 */
public interface FloodEventProcessorService {

    void processBatch(List<ProcessedSensorData> dataList);
}
