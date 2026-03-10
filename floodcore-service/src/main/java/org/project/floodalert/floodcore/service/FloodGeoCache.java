package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.dto.response.ActiveFloodResponse;

import java.util.List;

public interface FloodGeoCache {

    /**
     * Lưu hoặc cập nhật điểm ngập
     * <ol>
     *   <li>GEOADD vào GEO index</li>
     *   <li>HSET vào Hash detail + set TTL</li>
     * </ol>
     *
     * @param event sự kiện lifecycle chứa tọa độ và metadata
     */
    void cacheActiveFlood(FloodLifecycleEvent event);

    /**
     * Xóa điểm ngập khỏi cache.
     *
     * @param eventId ID sự kiện ngập cần xóa
     */
    void removeFloodCache(String eventId);

    List<ActiveFloodResponse> findFloodsInRadius(double lat, double lon, double radiusKm);

    void clearGeoIndex();
}

