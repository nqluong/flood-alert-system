package org.project.floodalert.floodprocessor.service.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.dto.event.ReportStatusUpdateEvent;
import org.project.floodalert.floodprocessor.dto.event.ReputationUpdateEvent;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.project.floodalert.floodprocessor.dto.response.ScoringResult;
import org.project.floodalert.floodprocessor.enums.ContributorRole;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.enums.ReputationReason;
import org.project.floodalert.floodprocessor.messaging.publisher.ReputationEventPublisher;
import org.project.floodalert.floodprocessor.model.EventContributor;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.service.cache.impl.SharedRedisGeoService;
import org.project.floodalert.floodprocessor.service.persistence.FloodEventPersistenceService;
import org.project.floodalert.floodprocessor.service.scoring.impl.ReportScoringEngine;
import org.project.floodalert.floodprocessor.utils.GeoHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

        private static final int POINTS_AUTO_REJECTED = -2;
        private static final int POINTS_PIONEER_APPROVED = 5;
        private static final int POINTS_VERIFIER_APPROVED = 2;
        private static final int POINTS_ACTIVE_CONFIRMATION = 2;

        private final SharedRedisGeoService redisGeoService;
        private final ReportScoringEngine scoringEngine;
        private final FloodEventPersistenceService persistenceService;
        private final StringRedisTemplate stringRedisTemplate;
        private final KafkaTemplate<String, Object> kafkaTemplate;
        private final ReputationEventPublisher reputationEventPublisher;

        @Value("${app.kafka.topic.lifecycle}")
        private String lifecycleTopic;

        @Value("${app.kafka.topic.report-status:report-status-update}")
        private String reportStatusTopic;

        /**
         * Entry point: Xử lý UserReportEvent từ Kafka.
         *
         * <p>
         * Luồng xử lý:
         * </p>
         * <ol>
         * <li><b>[Anti-Spam]:</b> Kiểm tra cooldown per user per geohash</li>
         * <li><b>[Routing]:</b> Tìm event lân cận và route theo status:
         * <ul>
         * <li>Không có lân cận -> Full scoring + Create new event</li>
         * <li>Có lân cận + ACTIVE -> Fast update (no scoring)</li>
         * <li>Có lân cận + PENDING -> Full scoring + Update</li>
         * </ul>
         * </li>
         * </ol>
         */
        @Override
        public void process(UserReportEvent event) {
                log.info("[REPORT-PROCESSOR] Nhận báo cáo: reportId={}, userId={}, lat={}, lon={} ═══",
                                event.getReportId(), event.getUserId(), event.getLat(), event.getLon());

                ReportMessage msg = toReportMessage(event);

                if (isSpamming(msg)) {
                        log.info("Người dùng spam quá nhiều");
                        return;
                }

                Optional<String> nearbyEventId = redisGeoService.findNearbyActiveFlood(
                                msg.getLat(), msg.getLon(), CLUSTER_RADIUS_KM);

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
         * <p>
         * Luồng:
         * </p>
         * <ol>
         * <li>Gọi ScoringEngine -> lấy ScoringResult (totalScore, aiScore, etc.)</li>
         * <li>Gọi PersistenceService.handleNewReport -> tạo FloodEvent +
         * TrustScore</li>
         * <li>Xử lý gamification theo status (REJECTED/PENDING/ACTIVE)</li>
         * <li>Bắn ReportStatusUpdateEvent về flood-core</li>
         * </ol>
         */
        private void handleNewReportScoring(ReportMessage msg) {
                // Chấm điểm full
                ScoringResult scoringResult = scoringEngine.evaluateScore(msg);
                double totalScore = scoringResult.getTotalScore();

                FloodEvent newEvent = persistenceService.handleNewReport(msg, totalScore);

                // GAMIFICATION: Scenario 1 & 2
                if (STATUS_REJECTED.equals(newEvent.getStatus())) {
                        // AUTO_REJECTED -> Penalty -2 điểm
                        log.info("[GAMIFICATION][SCENARIO-1] AUTO_REJECTED: reportId={}, userId={}, score={} → Penalty {}",
                                        msg.getReportId(), msg.getUserId(), totalScore, POINTS_AUTO_REJECTED);

                        publishReputationEvent(msg.getUserId(), newEvent.getEventId(),
                                        ReputationReason.AUTO_REJECTED, POINTS_AUTO_REJECTED, msg.getReportId());

                        // Bắn event cập nhật status = REJECTED về flood-core
                        publishReportStatusEvent(msg, STATUS_REJECTED, newEvent.getEventId(),
                                        "Điểm số quá thấp (score < 0.5)", totalScore);

                } else if (STATUS_PENDING.equals(newEvent.getStatus())) {
                        // PENDING -> Hold reward (Delayed Reward Mechanism)
                        log.info("[GAMIFICATION][SCENARIO-2] PENDING: reportId={}, userId={}, score={} → Hold reward, chờ approval",
                                        msg.getReportId(), msg.getUserId(), totalScore);

                        // Bắn event cập nhật status = PENDING về flood-core
                        publishReportStatusEvent(msg, STATUS_PENDING, newEvent.getEventId(), null, totalScore);

                } else if (STATUS_ACTIVE.equals(newEvent.getStatus())) {
                        // Event mới ngay lập tức ACTIVE (score >= 0.8) -> Reward PIONEER ngay
                        publishLifecycleEvent(newEvent, LifecycleEventType.CREATED);

                        log.info("[GAMIFICATION] ACTIVE ngay: reportId={}, userId={} → Reward {} (PIONEER)",
                                        msg.getReportId(), msg.getUserId(), POINTS_PIONEER_APPROVED);

                        publishReputationEvent(msg.getUserId(), newEvent.getEventId(),
                                        ReputationReason.CLUSTER_APPROVED, POINTS_PIONEER_APPROVED, msg.getReportId());

                        // Bắn event cập nhật status = APPROVED về flood-core
                        publishReportStatusEvent(msg, "APPROVED", newEvent.getEventId(), null, totalScore);

                        log.info("[REPORT-PROCESSOR][NEW-REPORT] Hoàn tất: eventId={}, status={}, score={}",
                                        newEvent.getEventId(), newEvent.getStatus(), totalScore);
                }
        }

        /**
         * Event lân cận có status = ACTIVE → Fast path (NO scoring).
         *
         * <p>
         * Luồng tối ưu:
         * </p>
         * <ol>
         * <li>KHÔNG gọi ScoringEngine (tiết kiệm AI API, time)</li>
         * <li>Gọi PersistenceService.handleFastUpdate → chỉ tăng vote_count</li>
         * <li>Gamification: Reward +2 điểm (ACTIVE_CONFIRMATION)</li>
         * <li>Bắn Kafka UPDATED event</li>
         * <li>Bắn ReportStatusUpdateEvent = APPROVED về flood-core</li>
         * </ol>
         */
        private void handleFastClusterUpdate(ReportMessage msg, String existingEventId) {
                log.info("[REPORT-PROCESSOR][FAST-UPDATE] Event đã ACTIVE. Skip scoring: reportId={} → eventId={}",
                                msg.getReportId(), existingEventId);

                // Cập nhật nhanh (không chấm điểm)
                FloodEvent updatedEvent = persistenceService.handleFastUpdate(msg, existingEventId);

                // GAMIFICATION: Scenario 4 - ACTIVE Confirmation (Té nước theo mưa)
                log.info("[GAMIFICATION][SCENARIO-4] ACTIVE confirmation: userId={}, eventId={} → Reward {}",
                                msg.getUserId(), existingEventId, POINTS_ACTIVE_CONFIRMATION);

                publishReputationEvent(msg.getUserId(), updatedEvent.getEventId(),
                                ReputationReason.ACTIVE_CONFIRMATION, POINTS_ACTIVE_CONFIRMATION, msg.getReportId());

                // Bắn Kafka UPDATED event
                publishLifecycleEvent(updatedEvent, LifecycleEventType.UPDATED);

                // Bắn event cập nhật status = APPROVED về flood-core (fast path không có score)
                publishReportStatusEvent(msg, "APPROVED", updatedEvent.getEventId(), null, null);

                log.info("[REPORT-PROCESSOR][FAST-UPDATE] Hoàn tất: eventId={}, vote_count={}",
                                updatedEvent.getEventId(), updatedEvent.getVoteCount());
        }

        /**
         * <b>KỊCH BẢN C:</b> Event lân cận có status = PENDING -> Full scoring để kiểm
         * tra PENDING->ACTIVE.
         *
         * <p>
         * Luồng:
         * </p>
         * <ol>
         * <li>Gọi ScoringEngine -> lấy điểm mới (vì vote_count tăng)</li>
         * <li>Gọi PersistenceService.handleClusterUpdate -> cập nhật điểm + check
         * transition</li>
         * <li>Nếu PENDING→ACTIVE: Reward TẤT CẢ contributors</li>
         * <li>Bắn Kafka UPDATED event</li>
         * <li>Bắn ReportStatusUpdateEvent về flood-core</li>
         * </ol>
         */
        private void handlePendingClusterUpdate(ReportMessage msg, String existingEventId) {
                log.info("[REPORT-PROCESSOR][PENDING-UPDATE] Event PENDING, re-score: reportId={} → eventId={}",
                                msg.getReportId(), existingEventId);

                // Lưu OLD status (trước khi update)
                String oldStatus = persistenceService.getEventStatus(existingEventId);

                // Chấm điểm lại
                ScoringResult scoringResult = scoringEngine.evaluateScore(msg);
                double newScore = scoringResult.getTotalScore();

                log.info("[REPORT-PROCESSOR][PENDING-UPDATE] Scoring result: eventId={}, newScore={}, " +
                                "ai={}, spatial={}, reputation={}",
                                existingEventId, newScore, scoringResult.getAiScore(),
                                scoringResult.getSpatialScore(), scoringResult.getReputationScore());

                // Cập nhật Database (có thể PENDING->ACTIVE)
                FloodEvent updatedEvent = persistenceService.handleClusterUpdate(msg, existingEventId, newScore);

                // GAMIFICATION: Scenario 3 - PENDING -> ACTIVE Transition
                if (STATUS_PENDING.equals(oldStatus) && STATUS_ACTIVE.equals(updatedEvent.getStatus())) {
                        log.info("[GAMIFICATION][SCENARIO-3] Status transition: PENDING → ACTIVE for eventId={}",
                                        existingEventId);

                        // Lấy TẤT CẢ contributors từ DB (PIONEER + all VERIFIERs)
                        List<EventContributor> contributors = persistenceService
                                        .getContributorsByEventId(updatedEvent.getId());
                        log.info("[GAMIFICATION][SCENARIO-3] Found {} contributors to reward", contributors.size());

                        // Reward từng contributor theo role
                        for (EventContributor contributor : contributors) {
                                String role = contributor.getRole();
                                int points;

                                if (ContributorRole.PIONEER.name().equals(role)) {
                                        points = POINTS_PIONEER_APPROVED; // +5 điểm
                                } else if (ContributorRole.VERIFIER.name().equals(role)) {
                                        points = POINTS_VERIFIER_APPROVED; // +2 điểm
                                } else {
                                        log.warn("[GAMIFICATION][SCENARIO-3] Unknown role: {} for contributor {}",
                                                        role, contributor.getId());
                                        continue;
                                }

                                log.info("[GAMIFICATION][SCENARIO-3] Reward: userId={}, role={}, points={}",
                                                contributor.getUserId(), role, points);

                                publishReputationEvent(contributor.getUserId(), updatedEvent.getEventId(),
                                                ReputationReason.CLUSTER_APPROVED, points, msg.getReportId());
                        }

                        // Bắn event cập nhật status = APPROVED về flood-core (PENDING -> ACTIVE)
                        publishReportStatusEvent(msg, "APPROVED", updatedEvent.getEventId(), null, newScore);

                } else {
                        // Vẫn còn PENDING -> giữ nguyên status PENDING
                        publishReportStatusEvent(msg, STATUS_PENDING, updatedEvent.getEventId(), null, newScore);
                }

                // Bắn Kafka UPDATED event
                publishLifecycleEvent(updatedEvent, LifecycleEventType.UPDATED);

                log.info("[REPORT-PROCESSOR][PENDING-UPDATE] Completed: eventId={}, status={}, newScore={}",
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
         * Convert UserReportEvent -> ReportMessage.
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

        /**
         * Helper: Publish ReputationUpdateEvent ra Kafka.
         * Non-critical operation - không throw exception để không phá main flow.
         */
        private void publishReputationEvent(UUID userId, String eventId,
                        ReputationReason reason, int points, String reportId) {
                try {
                        ReputationUpdateEvent event = ReputationUpdateEvent.builder()
                                        .userId(userId)
                                        .eventId(eventId)
                                        .reason(reason)
                                        .points(points)
                                        .reportId(reportId)
                                        .build();

                        reputationEventPublisher.publish(event);

                        log.debug("[REPUTATION] Published: userId={}, reason={}, points={}", userId, reason, points);

                } catch (Exception e) {
                        // Log-only, don't throw (gamification là phụ, không nên fail main flow)
                        log.error("[REPUTATION] Publish failed (non-critical): userId={}, reason={}: {}",
                                        userId, reason, e.getMessage(), e);
                }
        }

        private void publishReportStatusEvent(ReportMessage msg, String status,
                        String eventId, String rejectReason, Double score) {
                try {
                        ReportStatusUpdateEvent event = ReportStatusUpdateEvent.builder()
                                        .reportId(msg.getReportId())
                                        .userId(msg.getUserId())
                                        .status(status)
                                        .eventId(eventId)
                                        .rejectReason(rejectReason)
                                        .score(score)
                                        .build();

                        kafkaTemplate.send(reportStatusTopic, msg.getReportId(), event);

                        log.info("[REPORT-STATUS] Published: reportId={}, status={}, eventId={}, score={}",
                                        msg.getReportId(), status, eventId, score);

                } catch (Exception e) {
                        log.error("[REPORT-STATUS] Publish failed (non-critical): reportId={}, status={}: {}",
                                        msg.getReportId(), status, e.getMessage(), e);
                }
        }
}
