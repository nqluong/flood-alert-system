package org.project.floodalert.auth.service;

import java.util.UUID;

/**
 * Service quản lý static locations trong Redis Geo
 * Key: user:static_locations
 * Member format: {userId}::{zoneName}
 */
public interface StaticLocationRedisService {
    
    /**
     * Thêm hoặc cập nhật static location vào Redis
     * 
     * @param userId User ID
     * @param addressId Address ID (dùng để tạo unique member)
     * @param zoneName Tên zone (từ addressText hoặc addressType)
     * @param lat Vĩ độ
     * @param lon Kinh độ
     */
    void addOrUpdateLocation(UUID userId, UUID addressId, String zoneName, Double lat, Double lon);
    
    /**
     * Xóa static location khỏi Redis
     * 
     * @param userId User ID
     * @param addressId Address ID
     */
    void removeLocation(UUID userId, UUID addressId);
    
    /**
     * Xóa tất cả static locations của user
     * 
     * @param userId User ID
     */
    void removeAllUserLocations(UUID userId);
    
    /**
     * Sync tất cả địa chỉ từ database lên Redis
     * Dùng khi startup hoặc rebuild cache
     * 
     * @return Số lượng địa chỉ đã sync
     */
    long syncAllAddressesToRedis();
}
