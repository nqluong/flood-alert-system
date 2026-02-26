package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.ChangeStatusRequest;
import org.project.floodalert.floodcore.dto.request.DeleteSensorRequest;
import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.dto.response.ChangeStatusResponse;
import org.project.floodalert.floodcore.dto.response.DeleteSensorResponse;
import org.project.floodalert.floodcore.dto.response.UpdateSensorResponse;
import org.project.floodalert.floodcore.enums.SensorStatus;
import org.project.floodalert.floodcore.enums.SensorUpdateAction;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorUpdateServiceImpl implements SensorUpdateService {

    private final SensorRepository sensorRepository;
    private final SensorValidationService sensorValidationService;
    private final SensorChangeTrackingService sensorChangeTrackingService;
    private final SensorLogService sensorLogService;
    private final SensorRedisCache sensorRedisCache;
    private final SensorStatusTransitionService sensorStatusTransitionService;


    @Override
    @Transactional
    public UpdateSensorResponse updateSensor(UUID sensorId, UpdateSensorRequest request, UUID performedBy) {
        log.info("Bắt đầu update sensor: {}", sensorId);

        try {
            // Lấy sensor hiện tại
            Sensor oldSensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                            "Không tìm thấy sensor với ID: " + sensorId));

            String oldStatus = oldSensor.getStatus();

            // Detect changed fields
            List<String> changedFields = sensorChangeTrackingService.detectChangedFields(oldSensor, request);

            if (changedFields.isEmpty()) {
                log.info("Không có field nào thay đổi, bỏ qua update");
                return buildResponse(oldSensor, changedFields, "Không có thay đổi nào");
            }

            log.info("Phát hiện {} fields thay đổi: {}", changedFields.size(), changedFields);

            // Tạo snapshot OLD values (trước khi update)
            Map<String, Object> oldValueSnapshot = sensorChangeTrackingService.createOldValueSnapshot(
                    oldSensor, changedFields);

            // Validate nghiệp vụ
            validateUpdateRequest(request, oldSensor);

            applyChanges(oldSensor, request);
            Sensor updatedSensor = sensorRepository.save(oldSensor);

            // Tạo snapshot NEW values (sau khi update)
            Map<String, Object> newValueSnapshot = sensorChangeTrackingService.createNewValueSnapshot(
                    updatedSensor, changedFields);

            // Ghi audit log
            SensorUpdateAction action = SensorUpdateAction.determineAction(changedFields);
            String comment = buildComment(request.getComment(), action, changedFields);

            sensorLogService.logWithComment(
                    updatedSensor.getId(),
                    action.name(),
                    oldValueSnapshot,
                    newValueSnapshot,
                    comment,
                    performedBy
            );

            // Sync Redis
            sensorRedisCache.syncSensorToRedis(updatedSensor);
            if (!oldStatus.equals(updatedSensor.getStatus())) {
                logBlacklistChange(updatedSensor.getSensorId(), oldStatus, updatedSensor.getStatus());
            }
            invalidateCache(updatedSensor.getId());

            log.info("Update sensor {} thành công. Changed fields: {}",
                    updatedSensor.getSensorId(), changedFields);

            return buildResponse(updatedSensor, changedFields, "Cập nhật thành công");

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi update sensor {}: {}", sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể cập nhật cảm biến do lỗi hệ thống");
        }
    }

    @Override
    public UpdateSensorResponse updateSensorBySensorId(String sensorId, UpdateSensorRequest request, UUID performedBy) {
        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với mã: " + sensorId));

        return updateSensor(sensor.getId(), request, performedBy);
    }

    @Override
    @Transactional
    public DeleteSensorResponse softDelete(UUID sensorId, DeleteSensorRequest request, UUID performedBy) {
        try {
            // Lấy sensor hiện tại
            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                            "Không tìm thấy sensor với ID: " + sensorId));

            // Check trạng thái hiện tại
            if ("DELETED".equals(sensor.getStatus()) || "DISABLED".equals(sensor.getStatus())) {
                throw new AppException(CoreErrorCode.SENSOR_ALREADY_DELETED,
                        "Sensor đã bị xóa trước đó");
            }

            // Tạo snapshot old value
            Map<String, Object> oldValue = createSensorSnapshot(sensor);

            // Update status thành DISABLED
            sensor.setStatus(SensorStatus.DISABLED.name());
            Sensor updatedSensor = sensorRepository.save(sensor);

            // Tạo snapshot new value
            Map<String, Object> newValue = createSensorSnapshot(updatedSensor);

            // Ghi audit log
            String comment = buildDeleteComment(request.getReason(), false, request.getRemoveFromMap());
            sensorLogService.logWithComment(
                    updatedSensor.getId(),
                    "SENSOR_DISABLED",
                    oldValue,
                    newValue,
                    comment,
                    performedBy
            );

            // Update Redis
            boolean removedFromRedis = updateRedisForSoftDelete(updatedSensor, request.getRemoveFromMap());

            // Invalidate cache
            invalidateCache(updatedSensor.getId());
            return DeleteSensorResponse.builder()
                    .id(updatedSensor.getId())
                    .sensorId(updatedSensor.getSensorId())
                    .deleteType("SOFT_DELETE")
                    .status(updatedSensor.getStatus())
                    .message("Sensor đã được vô hiệu hóa. Data từ sensor này sẽ bị chặn.")
                    .deletedAt(LocalDateTime.now())
                    .removedFromMap(request.getRemoveFromMap())
                    .removedFromRedis(removedFromRedis)
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi soft delete sensor {}: {}", sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể xóa cảm biến do lỗi hệ thống");
        }
    }

    @Override
    public DeleteSensorResponse softDeleteBySensorId(String sensorId, DeleteSensorRequest request, UUID performedBy) {
        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với mã: " + sensorId));

        return softDelete(sensor.getId(), request, performedBy);
    }

    @Override
    public DeleteSensorResponse hardDelete(UUID sensorId, DeleteSensorRequest request, UUID performedBy) {
        try {

            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                            "Không tìm thấy sensor với ID: " + sensorId));

            Map<String, Object> oldValue = createSensorSnapshot(sensor);

            // Ghi audit log TRƯỚC KHI XÓA
            String comment = buildDeleteComment(request.getReason(), true, true);
            sensorLogService.logWithComment(
                    sensor.getId(),
                    "SENSOR_HARD_DELETED",
                    oldValue,
                    null,  // Không có new value vì đã xóa
                    comment,
                    performedBy
            );

            removeFromRedisCompletely(sensor);
            invalidateCache(sensor.getId());

            sensorRepository.delete(sensor);


            return DeleteSensorResponse.builder()
                    .id(sensor.getId())
                    .sensorId(sensor.getSensorId())
                    .deleteType("HARD_DELETE")
                    .status("PERMANENTLY_DELETED")
                    .message("Sensor đã bị xóa vĩnh viễn khỏi hệ thống. KHÔNG THỂ KHÔI PHỤC!")
                    .deletedAt(LocalDateTime.now())
                    .removedFromMap(true)
                    .removedFromRedis(true)
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi hard delete sensor {}: {}", sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể xóa vĩnh viễn cảm biến do lỗi hệ thống");
        }
    }

    @Override
    public DeleteSensorResponse hardDeleteBySensorId(String sensorId, DeleteSensorRequest request, UUID performedBy) {
        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với mã: " + sensorId));

        return hardDelete(sensor.getId(), request, performedBy);
    }

    @Override
    public DeleteSensorResponse restoreSensor(UUID sensorId, UUID performedBy) {
        try {
            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                            "Không tìm thấy sensor với ID: " + sensorId));

            if (!"DELETED".equals(sensor.getStatus()) && !"DISABLED".equals(sensor.getStatus())) {
                throw new AppException(CoreErrorCode.SENSOR_NOT_DELETED,
                        "Sensor không ở trạng thái DELETED/DISABLED, không thể restore");
            }

            Map<String, Object> oldValue = createSensorSnapshot(sensor);

            String oldStatus = sensor.getStatus();
            sensor.setStatus(SensorStatus.ACTIVE.name());
            Sensor restoredSensor = sensorRepository.save(sensor);

            // Snapshot mới
            Map<String, Object> newValue = createSensorSnapshot(restoredSensor);

            // Ghi log
            String comment = String.format("Khôi phục sensor từ trạng thái %s về ACTIVE", oldStatus);
            sensorLogService.logWithComment(
                    restoredSensor.getId(),
                    "SENSOR_RESTORED",
                    oldValue,
                    newValue,
                    comment,
                    performedBy
            );

            // Sync Redis
            sensorRedisCache.syncSensorToRedis(restoredSensor);

            // Invalidate cache
            invalidateCache(restoredSensor.getId());


            return DeleteSensorResponse.builder()
                    .id(restoredSensor.getId())
                    .sensorId(restoredSensor.getSensorId())
                    .deleteType("RESTORE")
                    .status(restoredSensor.getStatus())
                    .message("Sensor đã được khôi phục và có thể nhận data")
                    .deletedAt(LocalDateTime.now())
                    .removedFromMap(false)
                    .removedFromRedis(false)
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi restore sensor {}: {}", sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể khôi phục cảm biến do lỗi hệ thống");
        }
    }

    @Override
    public DeleteSensorResponse restoreSensorBySensorId(String sensorId, UUID performedBy) {
        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với mã: " + sensorId));

        return restoreSensor(sensor.getId(), performedBy);
    }

    @Override
    public ChangeStatusResponse changeStatus(UUID sensorId, ChangeStatusRequest request, UUID performedBy) {
        return sensorStatusTransitionService.changeStatus(sensorId, request, performedBy);
    }

    @Override
    public ChangeStatusResponse changeStatusBySensorId(String sensorId, ChangeStatusRequest request, UUID performedBy) {
        return sensorStatusTransitionService.changeStatusBySensorId(sensorId, request, performedBy);
    }

    private void validateUpdateRequest(UpdateSensorRequest request, Sensor currentSensor) {
        // Validate coordinates nếu có update
        if (request.getLat() != null || request.getLon() != null) {
            sensorValidationService.validateCoordinates(
                    request.getLat() != null ? request.getLat() : currentSensor.getLat(),
                    request.getLon() != null ? request.getLon() : currentSensor.getLon()
            );
        }

        // Validate thresholds nếu có update
        if (request.getWarningThreshold() != null || request.getDangerThreshold() != null) {
            sensorValidationService.validateThresholds(
                    request.getWarningThreshold() != null ?
                            request.getWarningThreshold() : currentSensor.getWarningThreshold(),
                    request.getDangerThreshold() != null ?
                            request.getDangerThreshold() : currentSensor.getDangerThreshold()
            );
        }
    }

    private void applyChanges(Sensor sensor, UpdateSensorRequest request) {
        if (request.getName() != null) {
            sensor.setName(request.getName());
        }

        if (request.getLocationName() != null) {
            sensor.setLocationName(request.getLocationName());
        }

        if (request.getLat() != null) {
            sensor.setLat(request.getLat());
        }

        if (request.getLon() != null) {
            sensor.setLon(request.getLon());
        }

        if (request.getWarningThreshold() != null) {
            sensor.setWarningThreshold(request.getWarningThreshold());
        }

        if (request.getDangerThreshold() != null) {
            sensor.setDangerThreshold(request.getDangerThreshold());
        }

        if (request.getHardwareModel() != null) {
            sensor.setHardwareModel(request.getHardwareModel());
        }

        if (request.getFirmwareVersion() != null) {
            sensor.setFirmwareVersion(request.getFirmwareVersion());
        }
    }

    private String buildComment(String userComment, SensorUpdateAction action, List<String> changedFields) {
        StringBuilder comment = new StringBuilder();

        comment.append(action.getDescription());
        comment.append(". Các trường đã thay đổi: ");
        comment.append(String.join(", ", changedFields));

        if (userComment != null && !userComment.isEmpty()) {
            comment.append(". Ghi chú: ").append(userComment);
        }

        return comment.toString();
    }

    private void invalidateCache(UUID sensorId) {
        try {
            sensorRedisCache.evictSensorCache(sensorId);
            log.debug("Đã xóa cache cho sensor: {}", sensorId);
        } catch (Exception e) {
            log.warn("Không thể xóa cache cho sensor {}: {}", sensorId, e.getMessage());
        }
    }

    private UpdateSensorResponse buildResponse(Sensor sensor, List<String> changedFields, String message) {
        return UpdateSensorResponse.builder()
                .id(sensor.getId())
                .sensorId(sensor.getSensorId())
                .name(sensor.getName())
                .locationName(sensor.getLocationName())
                .lat(sensor.getLat())
                .lon(sensor.getLon())
                .status(sensor.getStatus())
                .warningThreshold(sensor.getWarningThreshold())
                .dangerThreshold(sensor.getDangerThreshold())
                .hardwareModel(sensor.getHardwareModel())
                .firmwareVersion(sensor.getFirmwareVersion())
                .updatedAt(sensor.getUpdatedAt())
                .changedFields(changedFields)
                .message(message)
                .build();
    }

    private boolean updateRedisForSoftDelete(Sensor sensor, boolean removeFromMap) {
        try {
            if (removeFromMap) {
                sensorRedisCache.removeSensorFromRedis(sensor.getSensorId());
                return true;
            } else {
                sensorRedisCache.updateSensorStatus(sensor.getSensorId(), sensor.getStatus());
                return false;
            }
        } catch (Exception e) {
            log.error("Lỗi khi update Redis: {}", e.getMessage(), e);
            return false;
        }
    }

    private void removeFromRedisCompletely(Sensor sensor) {
        try {
            sensorRedisCache.removeSensorFromRedis(sensor.getSensorId());
        } catch (Exception e) {
            log.warn("Không thể xóa sensor {} khỏi Redis: {}",
                    sensor.getSensorId(), e.getMessage());
        }
    }
    private Map<String, Object> createSensorSnapshot(Sensor sensor) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", sensor.getId().toString());
        snapshot.put("sensorId", sensor.getSensorId());
        snapshot.put("name", sensor.getName());
        snapshot.put("status", sensor.getStatus());
        snapshot.put("lat", sensor.getLat());
        snapshot.put("lon", sensor.getLon());
        snapshot.put("warningThreshold", sensor.getWarningThreshold());
        snapshot.put("dangerThreshold", sensor.getDangerThreshold());
        return snapshot;
    }

    private String buildDeleteComment(String reason, boolean isHardDelete, boolean removeFromMap) {
        StringBuilder comment = new StringBuilder();

        if (isHardDelete) {
            comment.append("XÓA VĨNH VIỄN sensor khỏi hệ thống. ");
        } else {
            comment.append("Vô hiệu hóa sensor. ");
            if (removeFromMap) {
                comment.append("Đã xóa khỏi bản đồ. ");
            } else {
                comment.append("Vẫn hiển thị trên bản đồ nhưng không nhận data. ");
            }
        }

        comment.append("Lý do: ").append(reason);

        return comment.toString();
    }

    private void logBlacklistChange(String sensorId, String oldStatus, String newStatus) {
        boolean wasBlacklisted = isBlacklistStatus(oldStatus);
        boolean nowBlacklisted = isBlacklistStatus(newStatus);

        if (!wasBlacklisted && nowBlacklisted) {
            log.warn("Sensor {} được THÊM VÀO blacklist do status: {} → {}. " +
                            "Ingestion sẽ DROP data từ sensor này!",
                    sensorId, oldStatus, newStatus);
        } else if (wasBlacklisted && !nowBlacklisted) {
            log.info("Sensor {} được XÓA KHỎI blacklist do status: {} → {}. " +
                            "Ingestion có thể nhận data.",
                    sensorId, oldStatus, newStatus);
        }
    }

    private boolean isBlacklistStatus(String status) {
        return "DISABLED".equals(status) ||
                "DELETED".equals(status) ||
                "OFFLINE".equals(status);
    }
}
