package org.project.floodalert.floodprocessor.service;

import java.util.Map;
import java.util.Set;

public interface RedisCacheService {
    
    /**
     * Bulk fetch dữ liệu sensor từ Redis cho nhiều sensor IDs
     * 
     * @param sensorIds Set các sensor IDs cần fetch
     * @return Map với key là sensorId, value là Map chứa thông tin sensor từ Redis
     */
    Map<String, Map<String, String>> bulkFetchSensorInfo(Set<String> sensorIds);
    
    /**
     * Fetch thông tin của một sensor từ Redis
     * 
     * @param sensorId ID của sensor cần fetch
     * @return Map chứa thông tin sensor, hoặc null nếu không tồn tại
     */
    Map<String, String> getSensorInfo(String sensorId);
    
    /**
     * Kiểm tra Redis connection có hoạt động hay không
     * 
     * @return true nếu Redis đang hoạt động, false nếu không
     */
    boolean isRedisAvailable();
}
