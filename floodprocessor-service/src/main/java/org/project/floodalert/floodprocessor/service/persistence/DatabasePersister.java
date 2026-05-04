package org.project.floodalert.floodprocessor.service.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.project.floodalert.floodprocessor.repository.IotReadingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabasePersister {
    private final IotReadingRepository iotReadingRepository;

    @Transactional
    public List<IoTReading> batchSave(List<IoTReading> readings) {
        if (readings == null || readings.isEmpty()) {
            log.info("Không có dữ liệu để lưu vào cơ sở dữ liệu");
            return List.of();
        }

        try {
            List<IoTReading> iotReadings = iotReadingRepository.saveAll(readings);
            return iotReadings;
        }
        catch (Exception e) {
            throw new AppException(ProcessorErrorCode.PROCESSING_FAILED, "Không thể lưu dữ liệu vào database", e);
        }
    }

    public long count(){
        return iotReadingRepository.count();
    }
}
