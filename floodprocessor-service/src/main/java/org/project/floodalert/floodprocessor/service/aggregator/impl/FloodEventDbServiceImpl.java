package org.project.floodalert.floodprocessor.service.aggregator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.repository.IotReadingRepository;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class FloodEventDbServiceImpl implements FloodEventDbService {

    private static final int EVENT_TTL_MINUTES = 30;

    private static final DateTimeFormatter EVENT_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final FloodEventRepository floodEventRepository;
    private final IotReadingRepository iotReadingRepository;

    @Override
    @Transactional
    public FloodEventDbResult processAndSave(ProcessedSensorData data) {
        String sensorId = data.getSensorId();
        FloodStatus newStatus = data.getStatus();

        log.debug("Bắt đầu xử lý DB cho sensor [{}], trạng thái mới: {}", sensorId, newStatus);

        // Tra cứu sự kiện đang Active của sensor này
        Optional<FloodEvent> activeEventOpt =
                floodEventRepository.findActiveEventBySensorId(sensorId, LocalDateTime.now());

        FloodEventDbResult result = switch (newStatus) {
            case SAFE    -> handleScenarioA(data, activeEventOpt);
            case WARNING,
                 DANGER  -> handleScenarioBC(data, activeEventOpt, newStatus);
            default      -> {
                log.warn("Trạng thái không xác định [{}] cho sensor [{}], bỏ qua", newStatus, sensorId);
                yield FloodEventDbResult.noAction();
            }
        };

        // Back-link IoTReading nếu có FloodEvent được xử lý
        if (result.floodEvent() != null && data.getReadingId() != null) {
            backLinkIotReading(data.getReadingId(), result.floodEvent());
        }

        return result;
    }

    /**
     * Kịch bản A: Sensor gửi trạng thái SAFE.
     * - Nếu có sự kiện active Chuyển thành RESOLVED.
     * - Nếu không có sự kiện active → Không làm gì.
     */
    private FloodEventDbResult handleScenarioA(ProcessedSensorData data,
                                                Optional<FloodEvent> activeEventOpt) {
        if (activeEventOpt.isEmpty()) {
            log.debug("Sensor [{}] gửi SAFE nhưng không có sự kiện đang active, bỏ qua",
                    data.getSensorId());
            return FloodEventDbResult.noAction();
        }

        FloodEvent event = activeEventOpt.get();
        log.info("Kịch bản A – Nước rút: sensor [{}], sự kiện [{}] → RESOLVED",
                data.getSensorId(), event.getEventId());

        // Chuyển trạng thái sang RESOLVED và đặt expires_at = NOW()
        event.setStatus("RESOLVED");
        event.setExpiresAt(LocalDateTime.now());

        FloodEvent savedEvent = floodEventRepository.save(event);
        log.info("Đã lưu sự kiện RESOLVED [{}] vào DB", savedEvent.getEventId());

        return new FloodEventDbResult(savedEvent, LifecycleEventType.RESOLVED, true);
    }

    /**
     * Phân tách kịch bản B (Ngập mới) và C (Ngập kéo dài).
     */
    private FloodEventDbResult handleScenarioBC(ProcessedSensorData data,
                                                 Optional<FloodEvent> activeEventOpt,
                                                 FloodStatus newStatus) {
        if (activeEventOpt.isEmpty()) {
            return handleScenarioB(data, newStatus);
        } else {
            return handleScenarioC(data, activeEventOpt.get(), newStatus);
        }
    }

    /**
     * Kịch bản B: Ngập mới – sensor gửi WARNING/DANGER nhưng không có sự kiện active.
     * Tạo mới FloodEvent với status = CONFIRMED, confidence_score = 1.0, expires_at = +30 phút.
     */
    private FloodEventDbResult handleScenarioB(ProcessedSensorData data, FloodStatus newStatus) {
        String sensorId = data.getSensorId();
        String eventId = generateEventId(sensorId);

        log.info("Kịch bản B – Ngập mới: sensor [{}], tạo sự kiện mới [{}]", sensorId, eventId);

        LocalDateTime now = LocalDateTime.now();
        FloodEvent newEvent = FloodEvent.builder()
                .eventId(eventId)
                .source("SENSOR")
                .sourceId(sensorId)
                .lat(data.getLat())
                .lon(data.getLon())
                .locationDescription(data.getLocationName())
                .waterLevel(toBigDecimal(data.getWaterLevel()))
                .severityLevel(newStatus.name())
                .status("CONFIRMED")
                .confidenceScore(BigDecimal.ONE)
                .voteCount(1)
                .expiresAt(now.plusMinutes(EVENT_TTL_MINUTES))
                .build();

        FloodEvent savedEvent = floodEventRepository.save(newEvent);
        log.info("Đã lưu sự kiện ngập mới [{}] – mức nước: {}cm, khu vực: {}",
                savedEvent.getEventId(), savedEvent.getWaterLevel(), savedEvent.getLocationDescription());

        return new FloodEventDbResult(savedEvent, LifecycleEventType.CREATED, true);
    }

    /**
     * Kịch bản C: Ngập kéo dài – sensor gửi WARNING/DANGER và đã có sự kiện active.
     * - Cập nhật water_level nếu mức mới cao hơn.
     * - Gia hạn expires_at thêm 30 phút.
     * - Phát hiện ESCALATED nếu mức độ tăng (VD: WARNING → DANGER).
     */
    private FloodEventDbResult handleScenarioC(ProcessedSensorData data,
                                                FloodEvent activeEvent,
                                                FloodStatus newStatus) {
        String sensorId = data.getSensorId();
        String previousSeverity = activeEvent.getSeverityLevel();

        log.info("Kịch bản C – Ngập kéo dài: sensor [{}], sự kiện [{}]", sensorId, activeEvent.getEventId());

        // Cập nhật water_level nếu mức mới cao hơn mức cũ
        BigDecimal newWaterLevel = toBigDecimal(data.getWaterLevel());
        if (newWaterLevel != null
                && (activeEvent.getWaterLevel() == null
                    || newWaterLevel.compareTo(activeEvent.getWaterLevel()) > 0)) {
            log.debug("Cập nhật mức nước: {}cm → {}cm cho sự kiện [{}]",
                    activeEvent.getWaterLevel(), newWaterLevel, activeEvent.getEventId());
            activeEvent.setWaterLevel(newWaterLevel);
        }

        // Cập nhật mức độ cảnh báo theo trạng thái mới
        activeEvent.setSeverityLevel(newStatus.name());

        // Gia hạn thời gian hết hạn
        activeEvent.setExpiresAt(LocalDateTime.now().plusMinutes(EVENT_TTL_MINUTES));

        FloodEvent savedEvent = floodEventRepository.save(activeEvent);
        log.info("Đã gia hạn sự kiện [{}] đến {}", savedEvent.getEventId(), savedEvent.getExpiresAt());

        // Phát hiện ESCALATED: mức độ cảnh báo tăng lên
        boolean escalated = isEscalated(previousSeverity, newStatus);
        if (escalated) {
            log.info("Phát hiện leo thang: sự kiện [{}] từ [{}] → [{}]",
                    savedEvent.getEventId(), previousSeverity, newStatus.name());
            return new FloodEventDbResult(savedEvent, LifecycleEventType.ESCALATED, true);
        }

        // Chỉ gia hạn thời gian, không thay đổi mức độ → không publish Lifecycle Event
        log.debug("Sự kiện [{}] chỉ được gia hạn thời gian, không publish lifecycle event",
                savedEvent.getEventId());
        return new FloodEventDbResult(savedEvent, null, false);
    }

    /**
     * Cập nhật flood_event_id vào bảng iot_readings để liên kết ngược lại
     * bản ghi đọc cảm biến với sự kiện ngập tương ứng.
     */
    private void backLinkIotReading(String readingId, FloodEvent floodEvent) {
        try {
            iotReadingRepository.updateFloodEventIdByReadingId(floodEvent.getId(), readingId);
            log.debug("Back-link thành công: iot_reading [{}] → flood_event [{}]",
                    readingId, floodEvent.getEventId());
        } catch (Exception e) {
            // Back-linking thất bại không nên làm hỏng toàn bộ flow → chỉ log cảnh báo
            log.warn("Back-link thất bại cho reading [{}], flood_event [{}]: {}",
                    readingId, floodEvent.getEventId(), e.getMessage());
        }
    }

    /**
     * Tạo eventId theo định dạng: EV-yyyyMMdd-HHmmss-{sensorId}.
     * VD: EV-20260220-143025-SENSOR_001
     */
    private String generateEventId(String sensorId) {
        String timestamp = LocalDateTime.now().format(EVENT_ID_FORMATTER);
        return "EV-" + timestamp + "-" + sensorId;
    }

    /**
     * Kiểm tra xem mức độ cảnh báo có leo thang hay không.
     * So sánh `severity` (số nguyên) của trạng thái trước và mới.
     */
    private boolean isEscalated(String previousSeverity, FloodStatus newStatus) {
        if (previousSeverity == null) {
            return false;
        }
        try {
            FloodStatus prevEnum = FloodStatus.valueOf(previousSeverity);
            return newStatus.getSeverity() > prevEnum.getSeverity();
        } catch (IllegalArgumentException e) {
            log.warn("Không parse được severity cũ [{}], bỏ qua kiểm tra escalated", previousSeverity);
            return false;
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
