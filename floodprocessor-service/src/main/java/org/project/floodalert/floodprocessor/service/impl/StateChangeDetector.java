package org.project.floodalert.floodprocessor.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StateChangeDetector {

    public void detectAndUpdate(ProcessedSensorData data, FloodStatus previousStatus) {
        FloodStatus currentStatus = data.getStatus();
        String sensorId = data.getSensorId();

        data.setPreviousStatus(previousStatus);

        boolean hasChanged = hasStateChanged(currentStatus, previousStatus);
        data.setStateChanged(hasChanged);

        if (hasChanged) {
            logStateChange(sensorId, previousStatus, currentStatus);
        } else {
            log.trace("Sensor {} - Trạng thái không đổi: {}", sensorId, currentStatus);
        }
    }

    private boolean hasStateChanged(FloodStatus currentStatus, FloodStatus previousStatus) {
        if (previousStatus == null) {
            return true;
        }

        return currentStatus != previousStatus;
    }

    private void logStateChange(String sensorId, FloodStatus previousStatus, FloodStatus currentStatus) {
        if (previousStatus == null) {
            log.info("Sensor {} - Trạng thái ban đầu: {} (lần đầu tiên)",
                    sensorId, currentStatus);
        } else {
            log.info("Sensor {} - Thay đổi trạng thái: {} → {}",
                    sensorId, previousStatus, currentStatus);

            // Cảnh báo đặc biệt cho các thay đổi nguy hiểm
            if (currentStatus == FloodStatus.DANGER) {
                log.warn("CẢNH BÁO: Sensor {} chuyển sang trạng thái NGUY HIỂM!", sensorId);
            } else if (previousStatus == FloodStatus.DANGER && currentStatus != FloodStatus.DANGER) {
                log.info("Sensor {} đã thoát khỏi trạng thái NGUY HIỂM", sensorId);
            }
        }
    }
}
