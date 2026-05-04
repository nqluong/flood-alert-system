package org.project.floodalert.floodprocessor.service.assessment.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.mapper.SensorDataMapper;
import org.project.floodalert.floodprocessor.service.assessment.FloodAssessmentService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloodAssessmentServiceImpl implements FloodAssessmentService {

    private final FloodStatusCalculator floodStatusCalculator;
    private final SensorDataMapper sensorDataMapper;

    @Override
    public List<ProcessedSensorData> assessFloodStatus(List<EnrichedSensorData> enrichedDataList) {
        if (enrichedDataList == null) {
            throw new AppException(ProcessorErrorCode.ENRICHED_DATA_NULL, "enrichedData không được null");
        }

        if (enrichedDataList.isEmpty()) {
            return Collections.emptyList();
        }


        List<ProcessedSensorData> processedList = enrichedDataList.stream()
                .map(this::assessFloodStatus)
                .toList();

        // Log thống kê
        logProcessingStatistics(processedList);

        return processedList;
    }

    @Override
    public ProcessedSensorData assessFloodStatus(EnrichedSensorData enrichedData) {
        if (enrichedData == null) {
            throw new AppException(ProcessorErrorCode.ENRICHED_DATA_NULL, "enrichedData không được null");
        }

        String sensorId = enrichedData.getSensorId();

        FloodStatus status = floodStatusCalculator.calculate(
                enrichedData.getWaterLevel(),
                enrichedData.getWarningThreshold(),
                enrichedData.getDangerThreshold()
        );

        ProcessedSensorData processedData = sensorDataMapper.mapToProcessedData(enrichedData, status);

        return processedData;
    }

    private void logProcessingStatistics(List<ProcessedSensorData> processedList) {
        if (processedList.isEmpty()) {
            return;
        }

        long safeCount = processedList.stream()
                .filter(data -> data.getStatus() == FloodStatus.SAFE)
                .count();

        long warningCount = processedList.stream()
                .filter(data -> data.getStatus() == FloodStatus.WARNING)
                .count();

        long dangerCount = processedList.stream()
                .filter(data -> data.getStatus() == FloodStatus.DANGER)
                .count();

        long unknownCount = processedList.stream()
                .filter(data -> data.getStatus() == FloodStatus.UNKNOWN)
                .count();

        log.info("Thống kê trạng thái: SAFE={}, WARNING={}, DANGER={}, UNKNOWN={}",
                safeCount, warningCount, dangerCount, unknownCount);

        // Cảnh báo nếu có nhiều UNKNOWN
        if (unknownCount > 0) {
            log.warn("Có {} sensor với trạng thái UNKNOWN - Cần kiểm tra cấu hình ngưỡng",
                    unknownCount);
        }

        // Cảnh báo nếu có DANGER
        if (dangerCount > 0) {
            log.warn("CẢNH BÁO: Có {} sensor ở trạng thái DANGER - Cần xử lý khẩn cấp!",
                    dangerCount);
        }
    }
}
