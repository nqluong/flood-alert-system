package org.project.floodalert.notification.service;

import java.util.UUID;

public interface UserLocationSyncService {
    
    /**
     * Đồng bộ vị trí user vào Redis Geo để có thể quét được khi có điểm ngập gần đó
     *
     * @param userId User ID
     * @param latitude Vĩ độ
     * @param longitude Kinh độ
     */
    void syncUserLocationToRedis(UUID userId, Double latitude, Double longitude);

    /**
     * Xóa vị trí user khỏi Redis Geo
     *
     * @param userId User ID
     */
    void removeUserLocationFromRedis(UUID userId);
}
