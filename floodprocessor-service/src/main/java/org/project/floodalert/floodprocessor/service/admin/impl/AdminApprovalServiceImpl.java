package org.project.floodalert.floodprocessor.service.admin.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.dto.event.ReputationUpdateEvent;
import org.project.floodalert.floodprocessor.enums.ContributorRole;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.enums.ReputationReason;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.model.EventContributor;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.service.admin.AdminApprovalService;
import org.project.floodalert.floodprocessor.service.aggregator.LifecycleEventPublisher;
import org.project.floodalert.floodprocessor.service.gamification.ReputationEventPublisher;
import org.project.floodalert.floodprocessor.service.impl.FloodEventPersistenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApprovalServiceImpl implements AdminApprovalService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final BigDecimal ADMIN_APPROVED_SCORE = BigDecimal.valueOf(1.0);

    private static final int PIONEER_REWARD_POINTS = 5;
    private static final int VERIFIER_REWARD_POINTS = 2;
    private static final int REJECTION_PENALTY_POINTS = -10;

    private final FloodEventRepository floodEventRepository;
    private final FloodEventPersistenceService persistenceService;
    private final ReputationEventPublisher reputationPublisher;
    private final LifecycleEventPublisher lifecyclePublisher;

    @Override
    @Transactional
    public void approveEvent(String eventId) {
        log.info("[ADMIN-APPROVAL] Bắt đầu duyệt flood event: eventId={}", eventId);

        // 1. Tìm FloodEvent
        FloodEvent event = findFloodEventByEventId(eventId);
        log.info("[ADMIN-APPROVAL] Tìm thấy event: id={}, status={}, confidenceScore={}",
                event.getId(), event.getStatus(), event.getConfidenceScore());

        // 2. Update status = ACTIVE, confidenceScore = 1.0, confirmedAt = now
        event.setStatus(STATUS_ACTIVE);
        event.setConfidenceScore(ADMIN_APPROVED_SCORE);
        event.setConfirmedAt(LocalDateTime.now(ZoneId.systemDefault()));
        FloodEvent savedEvent = floodEventRepository.save(event);
        log.info("[ADMIN-APPROVAL] Đã update event: status={}, confidenceScore={}, confirmedAt={}",
                savedEvent.getStatus(), savedEvent.getConfidenceScore(), savedEvent.getConfirmedAt());

        // 3. Lấy tất cả contributors
        List<EventContributor> contributors = persistenceService.getContributorsByEventId(event.getId());
        log.info("[ADMIN-APPROVAL] Tìm thấy {} contributors", contributors.size());

        // 4. Bắn Kafka Reputation Event cho từng contributor
        for (EventContributor contributor : contributors) {
            int rewardPoints = calculateRewardPoints(contributor.getRole());

            ReputationUpdateEvent reputationEvent = ReputationUpdateEvent.builder()
                    .userId(contributor.getUserId())
                    .eventId(eventId)
                    .reason(ReputationReason.ADMIN_APPROVED)
                    .points(rewardPoints)
                    .reportId(null) // Admin approval không có reportId cụ thể
                    .timestamp(LocalDateTime.now())
                    .build();

            reputationPublisher.publish(reputationEvent);
            log.info("[ADMIN-APPROVAL] Đã bắn Reputation Event: userId={}, role={}, points={}",
                    contributor.getUserId(), contributor.getRole(), rewardPoints);
        }

        // 5. Bắn Kafka Lifecycle Event: UPDATED
        FloodLifecycleEvent lifecycleEvent = FloodLifecycleEvent.builder()
                .eventId(eventId)
                .type(LifecycleEventType.UPDATED)
                .waterLevel(savedEvent.getWaterLevel() != null ? savedEvent.getWaterLevel().doubleValue() : null)
                .severityLevel(savedEvent.getSeverityLevel())
                .location(savedEvent.getLocationDescription())
                .lat(savedEvent.getLat())
                .lon(savedEvent.getLon())
                .timestamp(LocalDateTime.now())
                .build();

        lifecyclePublisher.publish(lifecycleEvent);
        log.info("[ADMIN-APPROVAL] Đã bắn Lifecycle Event UPDATED cho eventId={}", eventId);
        log.info("[ADMIN-APPROVAL] Hoàn tất duyệt flood event: eventId={}", eventId);
    }

    @Override
    @Transactional
    public void rejectEvent(String eventId) {
        log.info("[ADMIN-REJECTION] Bắt đầu từ chối flood event: eventId={}", eventId);

        // 1. Tìm FloodEvent
        FloodEvent event = findFloodEventByEventId(eventId);
        log.info("[ADMIN-REJECTION] Tìm thấy event: id={}, status={}, confidenceScore={}",
                event.getId(), event.getStatus(), event.getConfidenceScore());

        // 2. Update status = REJECTED
        event.setStatus(STATUS_REJECTED);
        FloodEvent savedEvent = floodEventRepository.save(event);
        log.info("[ADMIN-REJECTION] Đã update event: status={}", savedEvent.getStatus());

        // 3. Lấy tất cả contributors
        List<EventContributor> contributors = persistenceService.getContributorsByEventId(event.getId());
        log.info("[ADMIN-REJECTION] Tìm thấy {} contributors để phạt điểm", contributors.size());

        // 4. Bắn Kafka Reputation Event phạt điểm cho từng contributor
        for (EventContributor contributor : contributors) {
            ReputationUpdateEvent reputationEvent = ReputationUpdateEvent.builder()
                    .userId(contributor.getUserId())
                    .eventId(eventId)
                    .reason(ReputationReason.ADMIN_REJECTED)
                    .points(REJECTION_PENALTY_POINTS)
                    .reportId(null) // Admin rejection không có reportId cụ thể
                    .timestamp(LocalDateTime.now())
                    .build();

            reputationPublisher.publish(reputationEvent);
            log.info("[ADMIN-REJECTION] Đã bắn Reputation Event penalty: userId={}, points={}",
                    contributor.getUserId(), REJECTION_PENALTY_POINTS);
        }

        log.info("[ADMIN-REJECTION] Hoàn tất từ chối flood event: eventId={}", eventId);
    }

    /**
     * Tìm FloodEvent theo eventId.
     * Throw AppException nếu không tìm thấy.
     */
    private FloodEvent findFloodEventByEventId(String eventId) {
        return floodEventRepository.findAll().stream()
                .filter(e -> e.getEventId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("[ADMIN-APPROVAL] Không tìm thấy FloodEvent: eventId={}", eventId);
                    return new AppException(
                            ProcessorErrorCode.FLOOD_EVENT_NOT_FOUND,
                            "FloodEvent not found with eventId: " + eventId
                    );
                });
    }

    /**
     * Tính điểm thưởng dựa trên role của contributor.
     * PIONEER: +5 điểm
     * VERIFIER: +2 điểm
     */
    private int calculateRewardPoints(String role) {
        if (ContributorRole.PIONEER.name().equals(role)) {
            return PIONEER_REWARD_POINTS;
        } else if (ContributorRole.VERIFIER.name().equals(role)) {
            return VERIFIER_REWARD_POINTS;
        }
        log.warn("[ADMIN-APPROVAL] Unknown role: {}, default to VERIFIER reward", role);
        return VERIFIER_REWARD_POINTS;
    }
}
