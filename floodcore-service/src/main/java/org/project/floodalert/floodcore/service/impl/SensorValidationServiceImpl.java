package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.SensorValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorValidationServiceImpl implements SensorValidationService {

    private final SensorRepository sensorRepository;

    @Override
    public void validateSensorIdNotExists(String sensorId) {
        if (sensorRepository.existsBySensorId(sensorId)) {
            log.warn("Sensor ID đã tồn tại: {}", sensorId);
            throw new AppException(CoreErrorCode.SENSOR_ALREADY_EXISTS,
                    "Cảm biến với mã " + sensorId + " đã tồn tại trong hệ thống");
        }
    }

    @Override
    public void validateThresholds(BigDecimal warningThreshold, BigDecimal dangerThreshold) {
        if (dangerThreshold.compareTo(warningThreshold) <= 0) {
            log.warn("Ngưỡng nguy hiểm ({}) phải lớn hơn ngưỡng cảnh báo ({})",
                    dangerThreshold, warningThreshold);
            throw new AppException(CoreErrorCode.INVALID_THRESHOLD);
        }
    }

    @Override
    public void validateCoordinates(BigDecimal lat, BigDecimal lon) {
        // Kiểm tra phạm vi vĩ độ
        if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 ||
                lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            log.warn("Vĩ độ không hợp lệ: {}", lat);
            throw new AppException(CoreErrorCode.INVALID_COORDINATES,
                    "Vĩ độ phải nằm trong khoảng -90 đến 90");
        }

        // Kiểm tra phạm vi kinh độ
        if (lon.compareTo(BigDecimal.valueOf(-180)) < 0 ||
                lon.compareTo(BigDecimal.valueOf(180)) > 0) {
            log.warn("Kinh độ không hợp lệ: {}", lon);
            throw new AppException(CoreErrorCode.INVALID_COORDINATES,
                    "Kinh độ phải nằm trong khoảng -180 đến 180");
        }
    }

    @Override
    public void validateCreateRequest(CreateSensorRequest request) {
        validateSensorIdNotExists(request.getSensorId());
        validateThresholds(request.getWarningThreshold(), request.getDangerThreshold());
        validateCoordinates(request.getLat(), request.getLon());

        log.info("Validate request tạo sensor thành công: {}", request.getSensorId());
    }
}
