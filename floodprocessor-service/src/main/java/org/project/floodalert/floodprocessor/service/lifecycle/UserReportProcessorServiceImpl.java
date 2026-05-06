package org.project.floodalert.floodprocessor.service.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserReportProcessorServiceImpl implements UserReportProcessorService {

    private final FloodEventRepository floodEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${system.config.user-report.pending-timeout-minutes:120}")
    private int pendingTimeoutMinutes;

    @Value("${system.config.user-report.decay-interval-minutes:30}")
    private int decayIntervalMinutes;

    @Value("${system.config.user-report.decay-amount:0.2}")
    private double decayAmount;

    @Value("${system.config.user-report.min-confidence-threshold:0.3}")
    private double minConfidenceThreshold;

    @Value("${system.config.user-report.sensor-crosscheck-radius-meters:50}")
    private double sensorCrosscheckRadiusMeters;

    @Value("${system.config.user-report.batch-size:100}")
    private int batchSize;

    private static final String KAFKA_TOPIC = "flood-lifecycle-events";

    @Override
    @Transactional
    public void cleanupPendingReports() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        log.info("Bắt đầu dọn dẹp báo cáo PENDING cũ hơn {} phút (cutoff: {})", 
                pendingTimeoutMinutes, cutoffTime);

        int totalRejected = 0;
        int pageNumber = 0;
        Page<FloodEvent> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            page = floodEventRepository.findPendingUserReportsOlderThan(cutoffTime, pageable);
            
            List<FloodEvent> reports = page.getContent();
            
            for (FloodEvent report : reports) {
                report.setStatus("REJECTED");
                floodEventRepository.save(report);
                totalRejected++;
                
                log.debug("Báo cáo ID {} đã bị REJECTED do vượt quá thời gian chờ xử lý", 
                        report.getEventId());
            }
            
            pageNumber++;
        } while (page.hasNext());

        log.info("Hoàn thành dọn dẹp báo cáo PENDING. Tổng số báo cáo bị REJECTED: {}", totalRejected);
    }

    @Override
    @Transactional
    public void applyTimeDecayAndSpatialCheck() {
        LocalDateTime decayCutoffTime = LocalDateTime.now().minusMinutes(decayIntervalMinutes);
        log.info("Bắt đầu áp dụng cơ chế lão hóa cho báo cáo ACTIVE không cập nhật từ {} phút trước (cutoff: {})", 
                decayIntervalMinutes, decayCutoffTime);

        int totalDecayed = 0;
        int totalKeptBySensor = 0;
        int totalResolved = 0;
        int pageNumber = 0;
        Page<FloodEvent> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            page = floodEventRepository.findActiveUserReportsWithoutRecentUpdate(decayCutoffTime, pageable);
            
            List<FloodEvent> reports = page.getContent();
            
            for (FloodEvent report : reports) {
                totalDecayed++;
                
                BigDecimal currentScore = report.getConfidenceScore();
                BigDecimal newScore = currentScore.subtract(BigDecimal.valueOf(decayAmount));
                report.setConfidenceScore(newScore);
                
                log.debug("Báo cáo ID {} bị trừ điểm: {} -> {}", 
                        report.getEventId(), currentScore, newScore);
                
                if (newScore.doubleValue() < minConfidenceThreshold) {
                    boolean hasSensorNearby = checkSensorNearby(report);
                    
                    if (hasSensorNearby) {
                        totalKeptBySensor++;
                        
                        if (report.getRawData() != null) {
                            report.getRawData().put("keptAliveBySensor", true);
                        }
                        
                        log.info("Báo cáo ID {} được giữ lại nhờ có Sensor Event trong bán kính {}m", 
                                report.getEventId(), sensorCrosscheckRadiusMeters);
                    } else {
                        totalResolved++;
                        report.setStatus("RESOLVED");
                        
                        log.info("Báo cáo ID {} đã được chuyển sang RESOLVED do không có Sensor Event xác nhận", 
                                report.getEventId());
                        
                        publishLifecycleEvent(report, LifecycleEventType.RESOLVED);
                    }
                }
                
                floodEventRepository.save(report);
            }
            
            pageNumber++;
        } while (page.hasNext());

        log.info("Hoàn thành cơ chế lão hóa. Tổng số báo cáo bị trừ điểm: {}, " +
                "Giữ lại nhờ sensor: {}, Chuyển sang RESOLVED: {}", 
                totalDecayed, totalKeptBySensor, totalResolved);
    }


    private boolean checkSensorNearby(FloodEvent report) {
        double radiusKm = sensorCrosscheckRadiusMeters / 1000.0;
        
        List<FloodEvent> nearbySensors = floodEventRepository.findActiveSensorEventsNearby(
                report.getLat(),
                report.getLon(),
                radiusKm
        );
        
        boolean hasNearby = !nearbySensors.isEmpty();
        
        if (hasNearby) {
            log.debug("Tìm thấy {} Sensor Event(s) ACTIVE trong bán kính {}m của báo cáo ID {}", 
                    nearbySensors.size(), sensorCrosscheckRadiusMeters, report.getEventId());
        }
        
        return hasNearby;
    }

    private void publishLifecycleEvent(FloodEvent event, LifecycleEventType type) {
        FloodLifecycleEvent lifecycleEvent = FloodLifecycleEvent.builder()
                .eventId(event.getEventId())
                .type(type)
                .location(event.getLocationDescription())
                .lat(event.getLat())
                .lon(event.getLon())
                .waterLevel(event.getWaterLevel() != null ? event.getWaterLevel().doubleValue() : 0.0)
                .severityLevel(event.getSeverityLevel() != null ? event.getSeverityLevel() : "UNKNOWN")
                .build();
        
        kafkaTemplate.send(KAFKA_TOPIC, lifecycleEvent);
        
        log.debug("Đã publish lifecycle event {} cho báo cáo ID {}", type, event.getEventId());
    }
}
