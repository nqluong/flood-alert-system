package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.mapper.IotReadingMapper;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.project.floodalert.floodprocessor.service.DispatcherService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherServiceImpl implements DispatcherService {

    private final IotReadingMapper iotReadingMapper;
    private final DatabasePersister databasePersister;
    private final KafkaDispatcher kafkaDispatcher;
    private final SensorHealthThrottlingService sensorHealthThrottlingService;

    @Override
    public void dispatch(List<ProcessedSensorData> processedSensorDataList) {
        if(processedSensorDataList == null || processedSensorDataList.isEmpty()) {
            log.info("Không có dữ liệu để phân phối");
            return;
        }
        List<IoTReading> entities = iotReadingMapper.toEntities(processedSensorDataList);
        databasePersister.batchSave(entities);
        dispatchToKafka(processedSensorDataList);
        syncSensorHealth(processedSensorDataList);
    }

    private void dispatchToKafka(List<ProcessedSensorData> processedDataList) {

        int successCount = 0;

        for (ProcessedSensorData data : processedDataList) {
            try {
                kafkaDispatcher.send(data);
                successCount++;
            } catch (Exception e) {
                log.error("Lỗi không mong đợi khi gửi Kafka cho sensor: {}",
                        data.getSensorId(), e);
            }
        }

        log.info("Đã dispatch {}/{} messages tới Kafka",
                successCount, processedDataList.size());

        // Cảnh báo nếu có messages fail
        int failedCount = processedDataList.size() - successCount;
        if (failedCount > 0) {
            log.warn("Có {}/{} messages không gửi được Kafka (đã lưu DB)",
                    failedCount, processedDataList.size());
        }
    }


    private void syncSensorHealth(List<ProcessedSensorData> processedDataList) {
        for (ProcessedSensorData data : processedDataList) {
            try {
                sensorHealthThrottlingService.checkAndSend(data);
            } catch (Exception e) {
                log.error("Lỗi không mong đợi khi sync sensor health cho sensor: {}",
                        data.getSensorId(), e);
            }
        }
    }
}

