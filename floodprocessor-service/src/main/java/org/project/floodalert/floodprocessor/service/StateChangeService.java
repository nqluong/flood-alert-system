package org.project.floodalert.floodprocessor.service;

import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;

import java.util.List;

public interface StateChangeService {
    /**
     * Phát hiện thay đổi trạng thái cho một batch sensors
     *
     * <p>Quy trình:</p>
     * <ol>
     *   <li>Fetch trạng thái cũ từ Redis (batch operation với multiGet)</li>
     *   <li>So sánh trạng thái hiện tại vs trạng thái cũ</li>
     *   <li>Cập nhật {@code previousStatus} và {@code stateChanged} vào DTO</li>
     *   <li>Lưu trạng thái mới vào Redis (batch operation với multiSet)</li>
     * </ol>
     *
     * <p><strong>Note:</strong> Method này modify input list in-place</p>
     *
     * @param processedDataList Danh sách dữ liệu đã được xử lý từ Module 2
     */
    void detectStateChanges(List<ProcessedSensorData> processedDataList);
}
