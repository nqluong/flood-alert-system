package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.project.floodalert.floodprocessor.dto.response.ScoringResult;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.service.ReportProcessingUseCase;
import org.project.floodalert.floodprocessor.service.SharedRedisGeoService;
import org.project.floodalert.floodprocessor.service.scoring.ReportScoringEngine;
import org.project.floodalert.floodprocessor.utils.GeoHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportProcessorServiceImpl implements ReportProcessingUseCase {

    private static final double CLUSTER_RADIUS_KM = 0.2;
    private static final String COOLDOWN_KEY_PREFIX = "flood:cooldown:";
    private static final long COOLDOWN_MINUTES = 15;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";

    private final SharedRedisGeoService redisGeoService;
    private final ReportScoringEngine scoringEngine;
    private final FloodEventPersistenceService persistenceService;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.lifecycle}")
    private String lifecycleTopic;

    /**
     * Entry point: Xử lý UserReportEvent từ Kafka.
     *
     * <p>Luồng xử lý:</p>
     * <ol>
     *   <li><b>[Chốt 1 - Anti-Spam]:</b> Kiểm tra cooldown per user per geohash</li>
     *   <li><b>[Chốt 2 - Routing]:</b> Tìm event lân cận và route theo status:
     *     <ul>
     *       <li>Không có lân cận → Full scoring + Create new event</li>
     *       <li>Có lân cận + ACTIVE → Fast update (no scoring)</li>
     *       <li>Có lân cận + PENDING → Full scoring + Update</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    @Override
    public void process(UserReportEvent event) {
        log.info("[REPORT-PROCESSOR] ═══ Nhận báo cáo: reportId={}, userId={}, lat={}, lon={} ═══",
                event.getReportId(), event.getUserId(), event.getLat(), event.getLon());

        ReportMessage msg = toReportMessage(event);

        if (isSpamming(msg)) {
            log.info("Người dùng spam quá nhiều");
            return;
        }

        Optional<String> nearbyEventId = redisGeoService.findNearbyActiveFlood(
                msg.getLat(), msg.getLon(), CLUSTER_RADIUS_KM
        );

        if (nearbyEventId.isEmpty()) {
            handleNewReportScoring(msg);
        } else {
            String existingEventId = nearbyEventId.get();
            String eventStatus = persistenceService.getEventStatus(existingEventId);

            log.info("[REPORT-PROCESSOR][ROUTING] Phát hiện event lân cận: eventId={}, status={}",
                    existingEventId, eventStatus);

            if (STATUS_ACTIVE.equals(eventStatus)) {
                // Status = ACTIVE -> Fast Path
                handleFastClusterUpdate(msg, existingEventId);
            } else if (STATUS_PENDING.equals(eventStatus)) {
                // Status = PENDING -> Full Scoring
                handlePendingClusterUpdate(msg, existingEventId);
            }
        }
    }


    private boolean isSpamming(ReportMessage msg) {
        String geohash = GeoHashUtil.encode(msg.getLat(), msg.getLon());
        String cooldownKey = COOLDOWN_KEY_PREFIX + msg.getUserId() + ":" + geohash;

        // Thử set key với TTL 15 phút. Nếu key đã tồn tại -> spam
        Boolean isNewReport = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "LOCKED", COOLDOWN_MINUTES, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isNewReport)) {
            return true;
        }

        return false;
    }

    /**
     * Không có sự kiện lân cận -> Chạy full scoring.
     *
     * <p>Luồng:</p>
     * <ol>
     *   <li>Gọi ScoringEngine -> lấy ScoringResult (totalScore, aiScore, etc.)</li>
     *   <li>Gọi PersistenceService.handleNewReport -> tạo FloodEvent + TrustScore</li>
     *   <li>Nếu status != REJECTED -> Bắn Kafka CREATED event</li>
     * </ol>
     */
    private void handleNewReportScoring(ReportMessage msg) {
        // Chấm điểm full
        ScoringResult scoringResult = scoringEngine.evaluateScore(msg);
        double totalScore = scoringResult.getTotalScore();

        FloodEvent newEvent = persistenceService.handleNewReport(msg, totalScore);

        //  Bắn Kafka event (nếu không bị REJECTED)
        if (!STATUS_REJECTED.equals(newEvent.getStatus())) {
            publishLifecycleEvent(newEvent, LifecycleEventType.CREATED);
            log.info("[REPORT-PROCESSOR][NEW-REPORT] Hoàn tất: eventId={}, status={}, totalScore={}",
                    newEvent.getEventId(), newEvent.getStatus(), totalScore);
        } else {
            log.info("[REPORT-PROCESSOR][NEW-REPORT] Báo cáo bị REJECTED: totalScore={} — Không bắn Kafka",
                    totalScore);
        }
    }

    /**
     * Event lân cận có status = ACTIVE → Fast path (NO scoring).
     *
     * <p>Luồng tối ưu:</p>
     * <ol>
     *   <li>KHÔNG gọi ScoringEngine (tiết kiệm AI API, time)</li>
     *   <li>Gọi PersistenceService.handleFastUpdate → chỉ tăng vote_count</li>
     *   <li>Bắn Kafka UPDATED event</li>
     * </ol>
     */
    private void handleFastClusterUpdate(ReportMessage msg, String existingEventId) {
        log.info("[REPORT-PROCESSOR][FAST-UPDATE] Event đã ACTIVE. Skip scoring: reportId={} → eventId={}",
                msg.getReportId(), existingEventId);

        // Cập nhật nhanh (không chấm điểm)
        FloodEvent updatedEvent = persistenceService.handleFastUpdate(msg, existingEventId);

        // Bắn Kafka UPDATED event
        publishLifecycleEvent(updatedEvent, LifecycleEventType.UPDATED);

        log.info("[REPORT-PROCESSOR][FAST-UPDATE] Hoàn tất: eventId={}, vote_count={}",
                updatedEvent.getEventId(), updatedEvent.getVoteCount());
    }

    /**
     * <b>KỊCH BẢN C:</b> Event lân cận có status = PENDING → Full scoring để kiểm tra PENDING→ACTIVE.
     *
     * <p>Luồng:</p>
     * <ol>
     *   <li>Gọi ScoringEngine → lấy điểm mới (vì vote_count tăng)</li>
     *   <li>Gọi PersistenceService.handleClusterUpdate → cập nhật điểm + check transition</li>
     *   <li>Bắn Kafka UPDATED event</li>
     * </ol>
     */
    private void handlePendingClusterUpdate(ReportMessage msg, String existingEventId) {
        log.info("[REPORT-PROCESSOR][PENDING-UPDATE] Event PENDING, cần re-score: reportId={} → eventId={}",
                msg.getReportId(), existingEventId);

        // Chấm điểm lại
        ScoringResult scoringResult = scoringEngine.evaluateScore(msg);
        double newScore = scoringResult.getTotalScore();

        log.info("[REPORT-PROCESSOR][PENDING-UPDATE] Kết quả scoring: eventId={}, newScore={}, " +
                        "ai={}, spatial={}, reputation={}",
                existingEventId, newScore, scoringResult.getAiScore(),
                scoringResult.getSpatialScore(), scoringResult.getReputationScore());

        // Cập nhật Database (có thể PENDING→ACTIVE)
        FloodEvent updatedEvent = persistenceService.handleClusterUpdate(msg, existingEventId, newScore);

        // Bắn Kafka UPDATED event
        publishLifecycleEvent(updatedEvent, LifecycleEventType.UPDATED);

        log.info("[REPORT-PROCESSOR][PENDING-UPDATE] Hoàn tất: eventId={}, status={}, newScore={}",
                updatedEvent.getEventId(), updatedEvent.getStatus(), newScore);
    }


    private void publishLifecycleEvent(FloodEvent event, LifecycleEventType type) {
        FloodLifecycleEvent lifecycleEvent = FloodLifecycleEvent.builder()
                .eventId(event.getEventId())
                .type(type)
                .lat(event.getLat())
                .lon(event.getLon())
                .build();

        kafkaTemplate.send(lifecycleTopic, event.getEventId(), lifecycleEvent);

        log.debug("[REPORT-PROCESSOR][KAFKA] Bắn {} event: eventId={} → topic={}",
                type, event.getEventId(), lifecycleTopic);
    }

    /**
     * Convert UserReportEvent → ReportMessage.
     */
    private ReportMessage toReportMessage(UserReportEvent event) {
        return ReportMessage.builder()
                .reportId(event.getReportId())
                .userId(event.getUserId())
                .lat(event.getLat())
                .lon(event.getLon())
                .imageUrl(event.getImageUrl())
                .severityLevel(event.getSeverityLevel())
                .description(event.getDescription())
                .build();
    }
}
