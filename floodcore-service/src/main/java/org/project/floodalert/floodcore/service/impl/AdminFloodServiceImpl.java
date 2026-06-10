package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.client.FloodProcessorClient;
import org.project.floodalert.floodcore.dto.response.AdminActiveFloodResponse;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.model.CoreActiveFlood;
import org.project.floodalert.floodcore.model.UserReport;
import org.project.floodalert.floodcore.repository.CoreActiveFloodRepository;
import org.project.floodalert.floodcore.repository.UserReportRepository;
import org.project.floodalert.floodcore.service.AdminFloodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFloodServiceImpl implements AdminFloodService {
    private final CoreActiveFloodRepository coreActiveFloodRepository;
    private final UserReportRepository userReportRepository;
    private final FloodProcessorClient floodProcessorClient;

    @Transactional(readOnly = true)
    @Override
    public List<AdminActiveFloodResponse> getAllActiveFloods() {
        List<CoreActiveFlood> entities = coreActiveFloodRepository.findAll();
        return entities.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void approveReport(UUID userReportId) {
        UserReport report = userReportRepository.findById(userReportId)
                .orElseThrow(() -> new AppException(CoreErrorCode.USER_REPORT_NOT_FOUND,
                        "Không tìm thấy báo cáo: " + userReportId));

        if (report.getFloodEventId() == null) {
            throw new AppException(CoreErrorCode.REPORT_NOT_LINKED_TO_EVENT,
                    "Báo cáo chưa được xử lý bởi hệ thống, vui lòng thử lại sau");
        }

        String floodEventId = report.getFloodEventId();
        log.info("[ADMIN-FLOOD-SERVICE] Approve report: userReportId={}, floodEventId={}", userReportId, floodEventId);

        report.setStatus("APPROVED");
        report.setReviewedAt(LocalDateTime.now());
        userReportRepository.save(report);
        log.info("[ADMIN-FLOOD-SERVICE] Đã cập nhật UserReport status=APPROVED: userReportId={}", userReportId);

        floodProcessorClient.approveFloodEvent(floodEventId);
    }

    @Override
    @Transactional
    public void rejectReport(UUID userReportId) {
        UserReport report = userReportRepository.findById(userReportId)
                .orElseThrow(() -> new AppException(CoreErrorCode.USER_REPORT_NOT_FOUND,
                        "Không tìm thấy báo cáo: " + userReportId));

        report.setStatus("REJECTED");
        report.setRejectReason("Bị từ chối bởi admin");
        report.setReviewedAt(LocalDateTime.now());
        userReportRepository.save(report);

        if (report.getFloodEventId() == null) {
            log.info("[ADMIN-FLOOD-SERVICE] Reject report (no floodEventId): userReportId={}", userReportId);
            return;
        }

        String floodEventId = report.getFloodEventId();
        log.info("[ADMIN-FLOOD-SERVICE] Reject report: userReportId={}, floodEventId={}", userReportId, floodEventId);
        floodProcessorClient.rejectFloodEvent(floodEventId);
        log.info("[ADMIN-FLOOD-SERVICE] Đã cập nhật UserReport status=REJECTED: userReportId={}", userReportId);
    }

    @Override
    public void dismissFlood(String eventId) {
        log.info("[ADMIN-FLOOD-SERVICE] Admin xóa điểm ngập: eventId={}", eventId);
        floodProcessorClient.dismissFloodEvent(eventId);
        log.info("[ADMIN-FLOOD-SERVICE] Đã gửi yêu cầu xóa điểm ngập tới floodprocessor: eventId={}", eventId);
    }

    private AdminActiveFloodResponse mapToResponse(CoreActiveFlood entity) {
        return AdminActiveFloodResponse.builder()
                .eventId(entity.getEventId())
                .lat(entity.getLat() != null ? entity.getLat().doubleValue() : null)
                .lon(entity.getLon() != null ? entity.getLon().doubleValue() : null)
                .location(entity.getLocationDescription())
                .waterLevel(entity.getWaterLevel() != null ? entity.getWaterLevel().doubleValue() : null)
                .severityLevel(entity.getSeverityLevel())
                .status(entity.getStatus())
                .source(entity.getSource())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
