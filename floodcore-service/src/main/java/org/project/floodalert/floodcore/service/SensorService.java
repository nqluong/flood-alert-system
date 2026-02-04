package org.project.floodalert.floodcore.service;

import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.response.CreateSensorResponse;
import org.project.floodalert.floodcore.dto.response.SensorDetailResponse;
import org.project.floodalert.floodcore.dto.response.SensorMapResponse;
import org.project.floodalert.floodcore.dto.response.SensorSummaryResponse;

import java.util.UUID;

public interface SensorService {
    /**
     * Tạo mới sensor
     * @param request Thông tin sensor cần tạo
     * @param performedBy ID người thực hiện
     * @return Thông tin sensor vừa tạo kèm API Key
     */
    CreateSensorResponse createSensor(CreateSensorRequest request, UUID performedBy);

    /**
     * Lấy danh sách sensor với phân trang và filter
     * Tự động sử dụng cache nếu có
     */
    PageResponse<SensorSummaryResponse> getSensorList(SensorFilterRequest filter);

    /**
     * Lấy chi tiết đầy đủ của sensor
     * @param sensorId ID của sensor
     * @param includeLogs Có lấy kèm lịch sử logs không
     */
    SensorDetailResponse getSensorDetail(UUID sensorId, boolean includeLogs);

    /**
     * Lấy chi tiết sensor theo sensorId (string)
     */
    SensorDetailResponse getSensorDetailBySensorId(String sensorId, boolean includeLogs);

    /**
     * Lấy dữ liệu GeoJSON cho bản đồ
     * Tối ưu bằng cách đọc từ Redis trước
     */
    SensorMapResponse getSensorMapData();

    /**
     * Lấy dữ liệu GeoJSON cho sensor đang active
     */
    SensorMapResponse getActiveSensorMapData();
}
