package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.dto.response.SafeRouteResponse;

public interface SafeRoutingService {

    /**
     * Tìm đường đi an toàn từ điểm A đến điểm B, tránh các điểm ngập nặng.
     *
     * <p>Luồng xử lý:
     * <ol>
     *   <li>Tính Bounding Box bao trùm A và B + buffer</li>
     *   <li>Truy vấn Redis Geo để lấy danh sách ID điểm ngập trong bbox</li>
     *   <li>Truy vấn Redis Hash để lấy chi tiết (waterLevel) của các điểm ngập</li>
     *   <li>Lọc điểm ngập theo ngưỡng waterLevel dựa vào vehicleType</li>
     *   <li>Tạo Polygon hình vuông bao quanh các điểm ngập nguy hiểm</li>
     *   <li>Gọi OpenRouteService API với avoid_polygons</li>
     *   <li>Trả về GeoJSON route</li>
     * </ol>
     *
     * @param request thông tin điểm xuất phát, điểm đến và loại phương tiện
     * @return GeoJSON route từ ORS
     */
    SafeRouteResponse findSafeRoute(SafeRouteRequest request);
}
