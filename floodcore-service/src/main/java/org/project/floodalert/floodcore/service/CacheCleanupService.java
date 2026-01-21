package org.project.floodalert.floodcore.service;

public interface CacheCleanupService {

    /**
     * Xóa dữ liệu geo-location cũ
     */
    void cleanupSensorGeoData();
}
