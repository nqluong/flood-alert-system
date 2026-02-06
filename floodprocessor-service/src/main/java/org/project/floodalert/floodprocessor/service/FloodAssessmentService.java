package org.project.floodalert.floodprocessor.service;

import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;

import java.util.List;

public interface FloodAssessmentService {
    /**
     * Xử lý danh sách dữ liệu cảm biến đã làm giàu và đánh giá trạng thái lũ lụt
     *
     * @param enrichedDataList Danh sách dữ liệu cảm biến đã được làm giàu từ Module 1
     * @return Danh sách dữ liệu cảm biến đã được đánh giá trạng thái lũ lụt
     */
    List<ProcessedSensorData> assessFloodStatus(List<EnrichedSensorData> enrichedDataList);

    /**
     * Xử lý một bản ghi dữ liệu cảm biến và đánh giá trạng thái lũ lụt
     *
     * @param enrichedData Dữ liệu cảm biến đã được làm giàu
     * @return Dữ liệu cảm biến đã được đánh giá trạng thái lũ lụt
     * @throws IllegalArgumentException nếu enrichedData là null
     */
    ProcessedSensorData assessFloodStatus(EnrichedSensorData enrichedData);
}
