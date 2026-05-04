package org.project.floodalert.floodprocessor.service.state.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.repository.RedisStateRepository;
import org.project.floodalert.floodprocessor.service.state.StateChangeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StateChangeServiceImpl implements StateChangeService {

    private final RedisStateRepository redisStateRepository;
    private final StateChangeDetector stateChangeDetector;

    @Override
    public void detectStateChanges(List<ProcessedSensorData> processedDataList) {
        // Validation
        if (processedDataList == null) {
            log.error("Input processedDataList không được null");
            throw new IllegalArgumentException("processedDataList không được null");
        }

        if (processedDataList.isEmpty()) {
            log.warn("Input processedDataList rỗng, bỏ qua state change detection");
            return;
        }

        log.info("Bắt đầu phát hiện thay đổi trạng thái cho {} sensors", processedDataList.size());

        // Extract sensor IDs
        Set<String> sensorIds = extractSensorIds(processedDataList);

        // Batch fetch trạng thái cũ từ Redis
        Map<String, FloodStatus> previousStatusMap = redisStateRepository.batchGetPreviousStatuses(sensorIds);

        // Detect changes và prepare new statuses
        Map<String, FloodStatus> newStatusMap = processAndDetectChanges(
                processedDataList,
                previousStatusMap
        );

        // Batch save trạng thái mới vào Redis
        redisStateRepository.batchSaveNewStatuses(newStatusMap);
        log.info("Hoàn thành phát hiện thay đổi trạng thái cho {} sensors", processedDataList.size());
    }

    private Set<String> extractSensorIds(List<ProcessedSensorData> processedDataList) {
        return processedDataList.stream()
                .map(ProcessedSensorData::getSensorId)
                .collect(Collectors.toSet());
    }


    private Map<String, FloodStatus> processAndDetectChanges(
            List<ProcessedSensorData> processedDataList,
            Map<String, FloodStatus> previousStatusMap) {

        Map<String, FloodStatus> newStatusMap = new HashMap<>();

        for (ProcessedSensorData data : processedDataList) {
            String sensorId = data.getSensorId();

            // Lấy trạng thái cũ từ map
            FloodStatus previousStatus = previousStatusMap.get(sensorId);

            stateChangeDetector.detectAndUpdate(data, previousStatus);

            newStatusMap.put(sensorId, data.getStatus());
        }

        return newStatusMap;
    }


}
