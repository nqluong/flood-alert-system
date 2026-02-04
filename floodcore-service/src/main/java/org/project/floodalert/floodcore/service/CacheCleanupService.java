package org.project.floodalert.floodcore.service;

public interface CacheCleanupService {

    /**
     * Xóa dữ liệu blacklist cũ
     */
    void cleanupSensorBlacklist();
}
