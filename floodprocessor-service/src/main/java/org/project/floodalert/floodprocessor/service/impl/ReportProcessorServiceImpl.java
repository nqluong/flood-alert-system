package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodprocessor.dto.request.ReportMessage;
import org.project.floodalert.floodprocessor.dto.request.UserReportEvent;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.service.scoring.ReportScoringEngine;
import org.project.floodalert.floodprocessor.service.ReportProcessingUseCase;
import org.project.floodalert.floodprocessor.service.SharedRedisGeoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReportProcessorServiceImpl implements ReportProcessingUseCase {

    private static final double CLUSTER_RADIUS_KM = 0.2;

    private final SharedRedisGeoService redisGeoService;
    private final ReportScoringEngine scoringEngine;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.lifecycle}")
    private String lifecycleTopic;


    @Override
    public void process(UserReportEvent event) {
        log.info("[REPORT-PROCESSOR] Bắt đầu xử lý báo cáo: reportId={}, userId={}, lat={}, lon={}",
                event.getReportId(), event.getUserId(), event.getLat(), event.getLon());

        // Map Kafka payload sang internal domain object
        ReportMessage msg = toReportMessage(event);

        // Kiểm tra Redis xem có điểm ngập nào trong bán kính 200m không
        Optional<String> nearbyEventId = redisGeoService.findNearbyActiveFlood(
                msg.getLat(), msg.getLon(), CLUSTER_RADIUS_KM
        );

        // Early Exit — gom cụm vào sự kiện ngập đang tồn tại
        if (nearbyEventId.isPresent()) {
            handleClusterUpdate(msg, nearbyEventId.get());
            return;
        }

        // Không có sự kiện lân cận rồi đưa vào Scoring Engine
        handleNewReportScoring(msg);
    }

    /**
     * Gom báo cáo vào sự kiện ngập đang tồn tại: bắn Kafka event {@code UPDATED}
     * để gia hạn thời gian sống (TTL) của điểm ngập trên Redis.
     */
    private void handleClusterUpdate(ReportMessage msg, String existingEventId) {
        log.info("[REPORT-PROCESSOR] Gom cụm vào sự kiện cũ: reportId={} → eventId={}",
                msg.getReportId(), existingEventId);

        FloodLifecycleEvent lifecycleEvent = FloodLifecycleEvent.builder()
                .eventId(existingEventId)
                .type(LifecycleEventType.UPDATED)
                .lat(msg.getLat())
                .lon(msg.getLon())
                .build();

        kafkaTemplate.send(lifecycleTopic, existingEventId, lifecycleEvent);

        log.info("[REPORT-PROCESSOR] Đã bắn UPDATED lifecycle event cho eventId={} lên topic={}",
                existingEventId, lifecycleTopic);
    }

    private void handleNewReportScoring(ReportMessage msg) {
        log.info("[REPORT-PROCESSOR] Không tìm thấy điểm ngập lân cận. Chuyển reportId={} sang Scoring Engine.",
                msg.getReportId());

        double totalScore = scoringEngine.evaluateTotalScore(msg);

        // TODO: Dựa trên totalScore để quyết định có tạo FloodEvent mới, lưu DB hay không
        log.info("[REPORT-PROCESSOR] Tổng điểm báo cáo — reportId={}, userId={}, totalScore={}",
                msg.getReportId(), msg.getUserId(), totalScore);
    }

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
