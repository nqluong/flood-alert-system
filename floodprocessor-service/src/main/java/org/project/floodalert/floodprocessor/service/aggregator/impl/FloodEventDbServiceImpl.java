package org.project.floodalert.floodprocessor.service.aggregator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.FloodStatus;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.repository.IotReadingRepository;
import org.project.floodalert.floodprocessor.service.aggregator.FloodEventDbService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


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


    @Transactional
    public List<FloodEventDbResult> processAndSaveBatch(List<ProcessedSensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        
        List<String> sensorIds = dataList.stream()
                .map(ProcessedSensorData::getSensorId)
                .distinct()
                .toList();

        // Lấy tất cả active events trong 1 query duy nhất
        List<FloodEvent> activeEvents = floodEventRepository.findActiveEventsBySensorIds(sensorIds, now);
        
        Map<String, FloodEvent> activeEventMap = activeEvents.stream()
                .collect(Collectors.toMap(
                        FloodEvent::getSourceId,
                        event -> event,
                        (existing, replacement) -> existing
                ));

        log.debug("Batch query: Tìm thấy {} active events cho {} sensors", 
                activeEventMap.size(), sensorIds.size());

        // Xử lý từng sensor data
        List<FloodEventDbResult> results = new ArrayList<>();
        List<FloodEvent> eventsToSave = new ArrayList<>();
        List<BackLinkTask> backLinkTasks = new ArrayList<>();

        for (ProcessedSensorData data : dataList) {
            try {
                String sensorId = data.getSensorId();
                FloodStatus newStatus = data.getStatus();
                Optional<FloodEvent> activeEventOpt = Optional.ofNullable(activeEventMap.get(sensorId));

                FloodEventDbResult result = switch (newStatus) {
                    case SAFE -> handleScenarioA(data, activeEventOpt);
                    case WARNING, DANGER -> handleScenarioBC(data, activeEventOpt, newStatus);
                    default -> {
                        log.warn("Trạng thái không xác định [{}] cho sensor [{}], bỏ qua", 
                                newStatus, sensorId);
                        yield FloodEventDbResult.noAction();
                    }
                };

                results.add(result);

                if (result.floodEvent() != null) {
                    eventsToSave.add(result.floodEvent());
                    
                    if (data.getReadingId() != null) {
                        backLinkTasks.add(new BackLinkTask(data.getReadingId(), result.floodEvent()));
                    }
                }

            } catch (Exception e) {
                log.error("Lỗi xử lý sensor [{}] trong batch: {}", 
                        data.getSensorId(), e.getMessage(), e);
                results.add(FloodEventDbResult.noAction());
            }
        }

        if (!eventsToSave.isEmpty()) {
            floodEventRepository.saveAll(eventsToSave);
            log.debug("Batch save: Lưu {} flood events", eventsToSave.size());
        }

        if (!backLinkTasks.isEmpty()) {
            batchBackLinkIotReadings(backLinkTasks);
        }

        return results;
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
        log.debug("Kịch bản A – Nước rút: sensor [{}], sự kiện [{}] → RESOLVED",
                data.getSensorId(), event.getEventId());

        event.setStatus("RESOLVED");
        event.setExpiresAt(LocalDateTime.now());

        return new FloodEventDbResult(event, LifecycleEventType.RESOLVED, true);
    }

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

        log.debug("Kịch bản B – Ngập mới: sensor [{}], tạo sự kiện mới [{}]", sensorId, eventId);

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

        return new FloodEventDbResult(newEvent, LifecycleEventType.CREATED, true);
    }

    /**
     * Kịch bản C: Ngập kéo dài – sensor gửi WARNING/DANGER và đã có sự kiện active.
     * - Cập nhật water_level nếu mức mới cao hơn.
     * - Gia hạn expires_at thêm 30 phút.
     * - Phát hiện ESCALATED nếu mức độ tăng (VD: WARNING → DANGER).
     * - Phát hiện DE_ESCALATED nếu mức độ giảm (VD: DANGER → WARNING).
     */
    private FloodEventDbResult handleScenarioC(ProcessedSensorData data,
                                                FloodEvent activeEvent,
                                                FloodStatus newStatus) {
        String sensorId = data.getSensorId();
        String previousSeverity = activeEvent.getSeverityLevel();

        log.debug("Kịch bản C – Ngập kéo dài: sensor [{}], sự kiện [{}]", sensorId, activeEvent.getEventId());

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

        activeEvent.setExpiresAt(LocalDateTime.now().plusMinutes(EVENT_TTL_MINUTES));

        SeverityChange severityChange = detectSeverityChange(previousSeverity, newStatus);
        
        return switch (severityChange) {
            case ESCALATED -> {
                log.info("Mức độ ngập gia tăng: {} → {} cho sự kiện [{}]",
                        previousSeverity, newStatus, activeEvent.getEventId());
                yield new FloodEventDbResult(activeEvent, LifecycleEventType.ESCALATED, true);
            }
            case DE_ESCALATED -> {
                log.info("Mức độ ngập giảm: {} → {} cho sự kiện [{}]",
                        previousSeverity, newStatus, activeEvent.getEventId());
                yield new FloodEventDbResult(activeEvent, LifecycleEventType.DE_ESCALATED, true);
            }
            case NO_CHANGE -> {
                log.debug("Sự kiện [{}] chỉ được gia hạn thời gian, không thay đổi severity",
                        activeEvent.getEventId());
                yield new FloodEventDbResult(activeEvent, null, false);
            }
        };
    }

    private void backLinkIotReading(String readingId, FloodEvent floodEvent) {
        try {
            iotReadingRepository.updateFloodEventIdByReadingId(floodEvent.getId(), readingId);
            log.debug("Back-link thành công: iot_reading [{}] → flood_event [{}]",
                    readingId, floodEvent.getEventId());
        } catch (Exception e) {
            log.warn("Back-link thất bại cho reading [{}], flood_event [{}]: {}",
                    readingId, floodEvent.getEventId(), e.getMessage());
        }
    }

    private void batchBackLinkIotReadings(List<BackLinkTask> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        try {
            List<String> readingIds = tasks.stream()
                    .map(BackLinkTask::readingId)
                    .toList();

            List<IoTReading> readings = iotReadingRepository.findAllByReadingIds(readingIds);

            Map<String, IoTReading> readingMap = readings.stream()
                    .collect(Collectors.toMap(IoTReading::getReadingId, r -> r));

            int updateCount = 0;
            for (BackLinkTask task : tasks) {
                IoTReading reading = readingMap.get(task.readingId);
                if (reading != null) {
                    reading.setFloodEventId(task.floodEvent.getId());
                    updateCount++;
                } else {
                    log.warn("Không tìm thấy IoTReading với readingId: {}", task.readingId);
                }
            }

            if (updateCount > 0) {
                iotReadingRepository.saveAll(readings);
            }

        } catch (Exception e) {
            log.error("Lỗi batch back-link, fallback sang update từng record: {}", e.getMessage());
            
            int successCount = 0;
            for (BackLinkTask task : tasks) {
                try {
                    iotReadingRepository.updateFloodEventIdByReadingId(
                            task.floodEvent.getId(), 
                            task.readingId
                    );
                    successCount++;
                } catch (Exception ex) {
                    log.warn("Back-link thất bại cho reading [{}]: {}", 
                            task.readingId, ex.getMessage());
                }
            }
            log.debug("Fallback back-link: {}/{} thành công", successCount, tasks.size());
        }
    }

    private String generateEventId(String sensorId) {
        String timestamp = LocalDateTime.now().format(EVENT_ID_FORMATTER);
        return "EV-" + timestamp + "-" + sensorId;
    }

    private enum SeverityChange {
        ESCALATED,      // Mức độ tăng
        DE_ESCALATED,   // Mức độ giảm
        NO_CHANGE       // Không thay đổi
    }

    private SeverityChange detectSeverityChange(String previousSeverity, FloodStatus newStatus) {
        if (previousSeverity == null) {
            return SeverityChange.NO_CHANGE;
        }
        
        try {
            FloodStatus prevEnum = FloodStatus.valueOf(previousSeverity);
            int prevSeverity = prevEnum.getSeverity();
            int newSeverity = newStatus.getSeverity();
            
            if (newSeverity > prevSeverity) {
                return SeverityChange.ESCALATED;
            } else if (newSeverity < prevSeverity) {
                return SeverityChange.DE_ESCALATED;
            } else {
                return SeverityChange.NO_CHANGE;
            }
        } catch (IllegalArgumentException e) {
            log.warn("Không parse được severity cũ [{}], coi như không thay đổi", previousSeverity);
            return SeverityChange.NO_CHANGE;
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private record BackLinkTask(String readingId, FloodEvent floodEvent) {}
}
