package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.dto.request.ChangeStatusRequest;
import org.project.floodalert.floodcore.dto.response.ChangeStatusResponse;
import org.project.floodalert.floodcore.enums.SensorStatus;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.repository.SensorRepository;
import org.project.floodalert.floodcore.service.SensorLogService;
import org.project.floodalert.floodcore.service.SensorRedisCache;
import org.project.floodalert.floodcore.service.SensorStatusTransitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorStatusTransitionServiceImpl implements SensorStatusTransitionService {

    private final SensorRepository sensorRepository;
    private final SensorLogService sensorLogService;
    private final SensorRedisCache sensorRedisCache;

    /**
     * Ma trận các chuyển đổi trạng thái hợp lệ.
     * DELETED không có transition nào (terminal state).
     */
    private static final Map<SensorStatus, Set<SensorStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(SensorStatus.class);
        ALLOWED_TRANSITIONS.put(SensorStatus.ACTIVE,
                EnumSet.of(SensorStatus.DISABLED, SensorStatus.MAINTENANCE, SensorStatus.OFFLINE));
        ALLOWED_TRANSITIONS.put(SensorStatus.DISABLED,
                EnumSet.of(SensorStatus.ACTIVE, SensorStatus.MAINTENANCE));
        ALLOWED_TRANSITIONS.put(SensorStatus.MAINTENANCE,
                EnumSet.of(SensorStatus.ACTIVE, SensorStatus.DISABLED, SensorStatus.OFFLINE));
        ALLOWED_TRANSITIONS.put(SensorStatus.OFFLINE,
                EnumSet.of(SensorStatus.ACTIVE, SensorStatus.MAINTENANCE));
        ALLOWED_TRANSITIONS.put(SensorStatus.DELETED, EnumSet.noneOf(SensorStatus.class));
    }

    @Override
    @Transactional
    public ChangeStatusResponse changeStatus(UUID sensorId, ChangeStatusRequest request, UUID performedBy) {
        log.info("Bắt đầu chuyển trạng thái sensor UUID={}, newStatus={}, performedBy={}",
                sensorId, request.getNewStatus(), performedBy);

        try {
            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                            "Không tìm thấy sensor với ID: " + sensorId));

            return doChangeStatus(sensor, request, performedBy);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi chuyển trạng thái sensor {}: {}", sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể chuyển trạng thái cảm biến do lỗi hệ thống");
        }
    }

    @Override
    public ChangeStatusResponse changeStatusBySensorId(String sensorId,
                                                        ChangeStatusRequest request,
                                                        UUID performedBy) {
        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new AppException(CoreErrorCode.SENSOR_NOT_FOUND,
                        "Không tìm thấy sensor với mã: " + sensorId));

        try {
            return doChangeStatus(sensor, request, performedBy);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi chuyển trạng thái sensor {}: {}", sensorId, e.getMessage(), e);
            throw new AppException(CoreErrorCode.DATABASE_ERROR,
                    "Không thể chuyển trạng thái cảm biến do lỗi hệ thống");
        }
    }


    private ChangeStatusResponse doChangeStatus(Sensor sensor, ChangeStatusRequest request, UUID performedBy) {
        SensorStatus targetStatus = parseAndValidateStatus(request.getNewStatus());

        SensorStatus currentStatus = parseAndValidateStatus(sensor.getStatus());

        // Guard: không cho phép chuyển sang cùng trạng thái
        if (currentStatus == targetStatus) {
            throw new AppException(CoreErrorCode.SENSOR_STATUS_SAME,
                    String.format("Sensor đã ở trạng thái %s, không cần chuyển đổi", currentStatus.name()));
        }

        // Guard: kiểm tra ma trận chuyển đổi hợp lệ
        Set<SensorStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(SensorStatus.class));
        if (!allowed.contains(targetStatus)) {
            throw new AppException(CoreErrorCode.INVALID_STATUS_TRANSITION,
                    String.format("Không thể chuyển từ %s sang %s. Các trạng thái cho phép: %s",
                            currentStatus.name(), targetStatus.name(), allowed));
        }

        // Tạo snapshot old
        Map<String, Object> oldSnapshot = createStatusSnapshot(sensor);

        // Thực hiện chuyển trạng thái
        String oldStatusStr = sensor.getStatus();
        sensor.setStatus(targetStatus.name());
        Sensor saved = sensorRepository.save(sensor);

        // Tạo snapshot new
        Map<String, Object> newSnapshot = createStatusSnapshot(saved);

        // Ghi audit log
        String comment = buildStatusComment(currentStatus, targetStatus, request.getReason(), request.getComment());
        sensorLogService.logWithComment(
                saved.getId(),
                "STATUS_CHANGED",
                oldSnapshot,
                newSnapshot,
                comment,
                performedBy
        );

        // Đồng bộ Redis & evict cache
        boolean synced = syncRedisAfterStatusChange(saved, currentStatus, targetStatus);
        evictCache(saved.getId());

        log.info("Chuyển trạng thái sensor {} thành công: {} → {}",
                saved.getSensorId(), oldStatusStr, targetStatus.name());

        return buildResponse(saved, oldStatusStr, synced);
    }

    /** Parse chuỗi trạng thái, ném lỗi nếu không hợp lệ */
    private SensorStatus parseAndValidateStatus(String statusStr) {
        try {
            return SensorStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(CoreErrorCode.INVALID_SENSOR_STATUS,
                    String.format("Trạng thái '%s' không hợp lệ. Giá trị hợp lệ: %s",
                            statusStr, Arrays.toString(SensorStatus.values())));
        }
    }

    /**
     * Đồng bộ Redis theo loại chuyển đổi:
     * - Sang trạng thái không nhận data (DISABLED, OFFLINE) chỉ update status hash
     * - Sang trạng thái nhận data (ACTIVE, MAINTENANCE) sync toàn bộ metadata
     */
    private boolean syncRedisAfterStatusChange(Sensor sensor, SensorStatus oldStatus, SensorStatus newStatus) {
        try {
            if (newStatus.isAcceptData()) {
                // Trạng thái hoạt động: đồng bộ đầy đủ
                sensorRedisCache.syncSensorToRedis(sensor);
            } else {
                // Trạng thái không nhận data: chỉ update trường status
                sensorRedisCache.updateSensorStatus(sensor.getSensorId(), sensor.getStatus());
            }
            log.debug("Đã đồng bộ Redis cho sensor {}: {} → {}", sensor.getSensorId(), oldStatus, newStatus);
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ Redis cho sensor {}: {}", sensor.getSensorId(), e.getMessage(), e);
            return false;
        }
    }

    private void evictCache(UUID sensorId) {
        try {
            sensorRedisCache.evictSensorCache(sensorId);
        } catch (Exception e) {
            log.warn("Không thể evict cache sensor {}: {}", sensorId, e.getMessage());
        }
    }

    private Map<String, Object> createStatusSnapshot(Sensor sensor) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", sensor.getId().toString());
        snapshot.put("sensorId", sensor.getSensorId());
        snapshot.put("status", sensor.getStatus());
        snapshot.put("name", sensor.getName());
        return snapshot;
    }

    private String buildStatusComment(SensorStatus oldStatus, SensorStatus newStatus, String reason, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Chuyển trạng thái: %s (%s) sang %s (%s).",
                oldStatus.getDescription(), oldStatus.name(),
                newStatus.getDescription(), newStatus.name()));
        sb.append(String.format(" Lý do: %s", reason));
        if (comment != null && !comment.isBlank()) {
            sb.append(String.format(". Ghi chú: %s", comment));
        }
        return sb.toString();
    }

    private boolean isBlacklistStatus(String status) {
        return "DISABLED".equals(status) || "DELETED".equals(status) || "OFFLINE".equals(status);
    }

    private List<String> getAllowedNextStatuses(SensorStatus current) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(SensorStatus.class))
                .stream()
                .map(SensorStatus::name)
                .sorted()
                .toList();
    }

    private ChangeStatusResponse buildResponse(Sensor sensor, String previousStatus, boolean syncedToRedis) {
        SensorStatus prev = SensorStatus.valueOf(previousStatus);
        SensorStatus current = SensorStatus.valueOf(sensor.getStatus());
        return ChangeStatusResponse.builder()
                .id(sensor.getId())
                .sensorId(sensor.getSensorId())
                .name(sensor.getName())
                .previousStatus(previousStatus)
                .currentStatus(sensor.getStatus())
                .allowedNextStatuses(getAllowedNextStatuses(current))
                .syncedToRedis(syncedToRedis)
                .addedToBlacklist(isBlacklistStatus(sensor.getStatus()))
                .message(String.format("Chuyển trạng thái thành công: %s (%s) sang %s (%s)",
                        prev.getDescription(), prev.name(),
                        current.getDescription(), current.name()))
                .changedAt(LocalDateTime.now())
                .build();
    }
}
