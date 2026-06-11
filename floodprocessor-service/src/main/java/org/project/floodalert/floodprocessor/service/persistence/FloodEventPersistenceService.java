package org.project.floodalert.floodprocessor.service.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.enums.ContributorRole;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.model.EventContributor;
import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.project.floodalert.floodprocessor.model.TrustScore;
import org.project.floodalert.floodprocessor.repository.EventContributorRepository;
import org.project.floodalert.floodprocessor.repository.FloodEventRepository;
import org.project.floodalert.floodprocessor.repository.TrustScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class FloodEventPersistenceService {

    private static final String FLOOD_EVENT_SOURCE_USER_REPORT = "USER_REPORT";
    private static final double THRESHOLD_REJECTED = 0.3;
    private static final double THRESHOLD_ACTIVE = 0.8;
    private static final int INITIAL_VOTE_COUNT = 1;

    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final FloodEventRepository floodEventRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final EventContributorRepository eventContributorRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FloodEvent handleNewReport(ReportMessage msg, double totalScore) {
        log.info("[PERSISTENCE] Xử lý báo cáo mới: reportId={}, userId={}, totalScore={}, lat={}, lon={}",
                msg.getReportId(), msg.getUserId(), totalScore, msg.getLat(), msg.getLon());

        try {
            // Phân loại status
            String status = classifyStatus(totalScore);

            // Tạo FloodEvent mới
            FloodEvent floodEvent = createNewFloodEvent(msg, totalScore, status);
            FloodEvent savedEvent = floodEventRepository.save(floodEvent);

            // Tạo TrustScore mới
            TrustScore trustScore = createNewTrustScore(savedEvent, msg, totalScore);
            trustScoreRepository.save(trustScore);

            // AUTO-INSERT EventContributor: PIONEER (người báo cáo đầu tiên)
            createContributor(savedEvent.getId(), msg.getUserId(), ContributorRole.PIONEER, msg.getReportId());

            return savedEvent;

        } catch (Exception e) {
            log.error("[PERSISTENCE] Lỗi khi lưu báo cáo mới: reportId={}, totalScore={}: {}",
                    msg.getReportId(), totalScore, e.getMessage(), e);
            throw new AppException(
                    ProcessorErrorCode.DATABASE_PERSISTENCE_FAILED,
                    "Failed to save new flood report: " + e.getMessage(),
                    e
            );
        }
    }

    @Transactional
    public FloodEvent handleClusterUpdate(ReportMessage msg, String existingEventId, double newRecalculatedScore) {
        log.info("[PERSISTENCE] Xử lý cập nhật cụm: reportId={}, existingEventId={}, newScore={}, lat={}, lon={}",
                msg.getReportId(), existingEventId, newRecalculatedScore, msg.getLat(), msg.getLon());

        try {
            FloodEvent existingEvent = floodEventRepository.findByEventId(existingEventId)
                    .orElseThrow(() -> new AppException(
                            ProcessorErrorCode.FLOOD_EVENT_NOT_FOUND,
                            "FloodEvent not found with eventId: " + existingEventId
                    ));

            // Cập nhật tâm vùng ngập theo weighted centroid trước khi tăng vote_count
            int oldVoteCount = existingEvent.getVoteCount();
            double oldLat = existingEvent.getLat();
            double oldLon = existingEvent.getLon();
            double newLat = (oldLat * oldVoteCount + msg.getLat()) / (oldVoteCount + 1);
            double newLon = (oldLon * oldVoteCount + msg.getLon()) / (oldVoteCount + 1);
            existingEvent.setLat(newLat);
            existingEvent.setLon(newLon);

            int newVoteCount = oldVoteCount + 1;
            existingEvent.setVoteCount(newVoteCount);

            // Backfill địa chỉ cho event được tạo trước khi báo cáo có địa chỉ
            if ((existingEvent.getLocationDescription() == null || existingEvent.getLocationDescription().isBlank())
                    && msg.getAddress() != null && !msg.getAddress().isBlank()) {
                existingEvent.setLocationDescription(msg.getAddress());
            }

            log.info("[PERSISTENCE][CENTROID] PENDING event centroid updated: eventId={}, ({},{}) → ({},{})",
                    existingEventId,
                    String.format("%.6f", oldLat), String.format("%.6f", oldLon),
                    String.format("%.6f", newLat), String.format("%.6f", newLon));

            // Cập nhật confidence_score
            BigDecimal newConfidenceScore = BigDecimal.valueOf(newRecalculatedScore)
                    .setScale(2, RoundingMode.HALF_UP);
            existingEvent.setConfidenceScore(newConfidenceScore);

            // Kiểm tra chuyển từ PENDING sang ACTIVE
            if (STATUS_PENDING.equals(existingEvent.getStatus()) && newRecalculatedScore >= THRESHOLD_ACTIVE) {
                existingEvent.setStatus(STATUS_ACTIVE);
                existingEvent.setConfirmedAt(LocalDateTime.now(ZoneId.systemDefault()));
                log.info("[PERSISTENCE] [STATUS TRANSITION] PENDING → ACTIVE (newScore={}, confirmedAt={})",
                        newRecalculatedScore, existingEvent.getConfirmedAt());
            }

            // Cập nhật raw_data nếu có imageUrl mới
            if (msg.getImageUrl() != null && !msg.getImageUrl().isBlank()) {
                Map<String, Object> rawData = existingEvent.getRawData();
                if (rawData == null) {
                    rawData = new HashMap<>();
                }
                rawData.put("imageUrl", msg.getImageUrl());
                rawData.put("description", msg.getDescription());
                rawData.put("lastUpdatedAt", LocalDateTime.now());
                existingEvent.setRawData(rawData);
            }

            // Lưu FloodEvent đã cập nhật
            FloodEvent updatedEvent = floodEventRepository.save(existingEvent);
            log.info("[PERSISTENCE] [UPDATED] FloodEvent: id={}, eventId={}, status={}, vote_count={}, confidenceScore={}",
                    updatedEvent.getId(), updatedEvent.getEventId(), updatedEvent.getStatus(),
                    updatedEvent.getVoteCount(), updatedEvent.getConfidenceScore());

            // INSERT TrustScore MỚI (KHÔNG UPDATE cũ)
            TrustScore newTrustScore = createNewTrustScore(updatedEvent, msg, newRecalculatedScore);
            TrustScore savedTrustScore = trustScoreRepository.save(newTrustScore);
            log.info("[PERSISTENCE] [CREATED NEW TRUST_SCORE] id={}, floodEventId={}, finalScore={} (lịch sử tiến hóa)",
                    savedTrustScore.getId(), savedTrustScore.getFloodEventId(), savedTrustScore.getFinalScore());

            // Chỉ insert nếu chưa tồn tại để tránh duplicate
            createContributor(updatedEvent.getId(), msg.getUserId(), ContributorRole.VERIFIER, msg.getReportId());

            return updatedEvent;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PERSISTENCE] Lỗi khi cập nhật cụm: reportId={}, existingEventId={}, newScore={}: {}",
                    msg.getReportId(), existingEventId, newRecalculatedScore, e.getMessage(), e);
            throw new AppException(
                    ProcessorErrorCode.DATABASE_PERSISTENCE_FAILED,
                    "Failed to update flood event cluster: " + e.getMessage(),
                    e
            );
        }
    }

    private String classifyStatus(double totalScore) {
        if (totalScore < THRESHOLD_REJECTED) {
            return STATUS_REJECTED;
        } else if (totalScore < THRESHOLD_ACTIVE) {
            return STATUS_PENDING;
        } else {
            return STATUS_ACTIVE;
        }
    }

    private FloodEvent createNewFloodEvent(ReportMessage msg, double totalScore, String status) {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("imageUrl", msg.getImageUrl());
        rawData.put("description", msg.getDescription());
        rawData.put("userId", msg.getUserId().toString());
        rawData.put("receivedAt", msg.getReceivedAt());

        return FloodEvent.builder()
                .eventId(eventId)
                .source(FLOOD_EVENT_SOURCE_USER_REPORT)
                .lat(msg.getLat())
                .lon(msg.getLon())
                .locationDescription(msg.getAddress())
                .severityLevel(msg.getSeverityLevel())
                .status(status)
                .confidenceScore(BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP))
                .voteCount(INITIAL_VOTE_COUNT)
                .rawData(rawData)
                .build();
    }


    private TrustScore createNewTrustScore(FloodEvent floodEvent, ReportMessage msg, double totalScore) {
        Map<String, Object> factorsMap = new HashMap<>();
        factorsMap.put("totalScore", totalScore);
        factorsMap.put("userId", msg.getUserId().toString());
        factorsMap.put("reportId", msg.getReportId());
        factorsMap.put("lat", msg.getLat());
        factorsMap.put("lon", msg.getLon());
        factorsMap.put("severityLevel", msg.getSeverityLevel());
        factorsMap.put("timestamp", Instant.now());

        String factorsJson = convertMapToJson(factorsMap);

        // Tạo TrustScore
        return TrustScore.builder()
                .floodEventId(floodEvent.getId())
                .finalScore(BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP))
                .factors(factorsJson)
                .calculationMethod("SCORING_ENGINE")
                .algorithmVersion("1.0")
                .build();
    }

    public String getEventStatus(String eventId) {
        FloodEvent event = floodEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new AppException(
                        ProcessorErrorCode.FLOOD_EVENT_NOT_FOUND,
                        "FloodEvent not found with eventId: " + eventId
                ));

        log.debug("[PERSISTENCE] Lấy status của eventId={} → status={}", eventId, event.getStatus());
        return event.getStatus();
    }

    /**
     * Cập nhật nhanh FloodEvent khi status đã là ACTIVE.
     * Chỉ tăng vote_count và cập nhật timestamp, KHÔNG tính điểm mới.
     * Tối ưu cho trường hợp event đã được xác nhận.
     *
     * @param msg Thông tin báo cáo mới
     * @param existingEventId ID của FloodEvent đang tồn tại
     * @return FloodEvent sau khi cập nhật
     */
    @Transactional
    public FloodEvent handleFastUpdate(ReportMessage msg, String existingEventId) {
        log.info("[PERSISTENCE][FAST-UPDATE] Cập nhật nhanh cho ACTIVE event: reportId={}, eventId={}",
                msg.getReportId(), existingEventId);

        try {
            FloodEvent existingEvent = floodEventRepository.findByEventId(existingEventId)
                    .orElseThrow(() -> new AppException(
                            ProcessorErrorCode.FLOOD_EVENT_NOT_FOUND,
                            "FloodEvent not found with eventId: " + existingEventId
                    ));

            // Cập nhật tâm vùng ngập theo weighted centroid trước khi tăng vote_count
            int oldVoteCount = existingEvent.getVoteCount();
            double oldLat = existingEvent.getLat();
            double oldLon = existingEvent.getLon();
            double newLat = (oldLat * oldVoteCount + msg.getLat()) / (oldVoteCount + 1);
            double newLon = (oldLon * oldVoteCount + msg.getLon()) / (oldVoteCount + 1);
            existingEvent.setLat(newLat);
            existingEvent.setLon(newLon);
            existingEvent.setVoteCount(oldVoteCount + 1);

            log.info("[PERSISTENCE][CENTROID] ACTIVE event centroid updated: eventId={}, ({},{}) → ({},{})",
                    existingEventId,
                    String.format("%.6f", oldLat), String.format("%.6f", oldLon),
                    String.format("%.6f", newLat), String.format("%.6f", newLon));

            // AUTO-INSERT EventContributor: VERIFIER (xác nhận cho event ACTIVE)
            createContributor(existingEvent.getId(), msg.getUserId(), ContributorRole.VERIFIER, msg.getReportId());

            // Cập nhật image nếu có
            if (msg.getImageUrl() != null && !msg.getImageUrl().isBlank()) {
                Map<String, Object> rawData = existingEvent.getRawData();
                if (rawData == null) {
                    rawData = new HashMap<>();
                }
                rawData.put("imageUrl", msg.getImageUrl());
                rawData.put("description", msg.getDescription());
                rawData.put("lastUpdatedAt", LocalDateTime.now());
                existingEvent.setRawData(rawData);
            }

            // Lưu FloodEvent
            FloodEvent updated = floodEventRepository.save(existingEvent);
            log.info("[PERSISTENCE][FAST-UPDATE] Hoàn tất: eventId={}, vote_count={}",
                    updated.getEventId(), updated.getVoteCount());

            return updated;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PERSISTENCE][FAST-UPDATE] Lỗi: reportId={}, eventId={}: {}",
                    msg.getReportId(), existingEventId, e.getMessage(), e);
            throw new AppException(
                    ProcessorErrorCode.DATABASE_PERSISTENCE_FAILED,
                    "Failed to fast-update flood event: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Lấy tất cả contributors của một FloodEvent.
     * Dùng cho Scenario 3 (PENDING -> ACTIVE) để reward tất cả contributors.
     */
    public List<EventContributor> getContributorsByEventId(UUID floodEventId) {
        try {
            return eventContributorRepository.findByFloodEventIdOrderByCreatedAtDesc(floodEventId);


        } catch (Exception e) {
            log.error("[PERSISTENCE] Lỗi khi lấy contributors: floodEventId={}: {}",
                    floodEventId, e.getMessage(), e);
            throw new AppException(
                    ProcessorErrorCode.DATABASE_PERSISTENCE_FAILED,
                    "Failed to retrieve contributors: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Tạo EventContributor mới với duplicate check.
     * Nếu user đã contribute rồi, skip để tránh duplicate.
     */
    private void createContributor(UUID floodEventId, UUID userId, ContributorRole role, String reportId) {
        try {
            // Check duplicate
            Optional<EventContributor> existing = eventContributorRepository
                    .findByFloodEventIdAndUserId(floodEventId, userId);

            if (existing.isPresent()) {
                log.debug("[PERSISTENCE] User đã contribute rồi: floodEventId={}, userId={}, skip",
                        floodEventId, userId);
                return;
            }

            EventContributor contributor = EventContributor.builder()
                    .floodEventId(floodEventId)
                    .userId(userId)
                    .role(role.name()) // Convert enum to String
                    .reportId(reportId)
                    .createdAt(LocalDateTime.now())
                    .build();

            eventContributorRepository.save(contributor);
        } catch (Exception e) {
            // Không throw exception để không phá vỡ flow chính
            // Chỉ log error và tiếp tục
            log.error("[PERSISTENCE] Lỗi khi tạo contributor (non-critical): " +
                    "floodEventId={}, userId={}, role={}: {}",
                    floodEventId, userId, role, e.getMessage(), e);
        }
    }

    private String convertMapToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("[PERSISTENCE] Lỗi serialize factors map: {}", e.getMessage());
            return "{}";
        }
    }
}
