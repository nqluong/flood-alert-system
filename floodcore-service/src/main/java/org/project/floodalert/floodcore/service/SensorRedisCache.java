package org.project.floodalert.floodcore.service;

import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;
import org.project.floodalert.floodcore.model.Sensor;

import java.util.Optional;
import java.util.UUID;

public interface SensorRedisCache {
    /**
     * Đồng bộ thông tin sensor vào Redis sau khi tạo mới
     */
    void syncSensorToRedis(Sensor sensor);

    /**
     * Lưu metadata sensor vào Redis Hash
     */
    void saveSensorMetadata(Sensor sensor);

    /**
     * Lưu tọa độ sensor vào Redis Geo
     */
    void saveSensorLocation(Sensor sensor);

    /**
     * Xóa dữ liệu sensor khỏi Redis
     */
    void removeSensorFromRedis(String sensorId);

    /**
     * Cập nhật status sensor trong Redis
     */
    void updateSensorStatus(String sensorId, String status);

    /**
     * Lấy danh sách sensor từ cache
     * @return Optional của PageResponse, empty nếu cache miss
     */
    Optional<PageResponse<SensorSummaryResponse>> getCachedSensorList(SensorFilterRequest filter);

    /**
     * Lưu danh sách sensor vào cache
     */
    void cacheSensorList(SensorFilterRequest filter, PageResponse<SensorSummaryResponse> response);

    /**
     * Lấy chi tiết sensor từ cache
     */
    Optional<SensorDetailResponse> getCachedSensorDetail(UUID sensorId);

    /**
     * Lưu chi tiết sensor vào cache
     */
    void cacheSensorDetail(UUID sensorId, SensorDetailResponse response);

    /**
     * Lấy map data từ cache
     */
    Optional<SensorMapResponse> getCachedMapData();

    /**
     * Lưu map data vào cache
     */
    void cacheMapData(SensorMapResponse response);

    /**
     * Xóa cache của một sensor
     */
    void evictSensorCache(UUID sensorId);

    /**
     * Xóa toàn bộ cache liên quan đến sensor list
     */
    void evictAllListCache();

    /**
     * Xóa cache map data
     */
    void evictMapCache();

    /**
     * Thêm sensor vào blacklist (khi DISABLED/DELETED)
     * Ingestion Service sẽ check blacklist và drop data
     */
    void addToBlacklist(String sensorId);

    /**
     * Xóa sensor khỏi blacklist (khi ACTIVE/MAINTENANCE)
     */
    void removeFromBlacklist(String sensorId);

    /**
     * Check xem sensor có trong blacklist không
     */
    boolean isInBlacklist(String sensorId);

    /**
     * Đồng bộ blacklist status dựa trên sensor status
     * - DISABLED/DELETED/OFFLINE → Add to blacklist
     * - ACTIVE/MAINTENANCE → Remove from blacklist
     */
    void syncBlacklistStatus(String sensorId, String status);
}
