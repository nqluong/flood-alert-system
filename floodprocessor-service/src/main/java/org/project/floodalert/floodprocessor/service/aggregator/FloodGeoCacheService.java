package org.project.floodalert.floodprocessor.service.aggregator;

import org.project.floodalert.floodprocessor.model.FloodEvent;


public interface FloodGeoCacheService {

    /**
     * Đồng bộ một sự kiện ngập đang active vào Redis (GEOADD + HSET).
     * Được gọi khi Kịch bản B (Ngập mới) hoặc Kịch bản C (Ngập kéo dài).
     *
     * @param floodEvent sự kiện ngập đã được persist (có đầy đủ eventId, lat, lon, …)
     */
    void syncActiveFloodEvent(FloodEvent floodEvent);

    /**
     * Xóa một sự kiện ngập đã RESOLVED khỏi Redis (ZREM + HDEL).
     * Được gọi khi Kịch bản A (Nước rút).
     *
     * @param eventId ID của flood event cần xóa khỏi cache
     */
    void removeResolvedFloodEvent(String eventId);
}
