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
    public void approveReport(UUID userReportId) {
        String floodEventId = resolveFloodEventId(userReportId);
        log.info("[ADMIN-FLOOD-SERVICE] Approve report: userReportId={}, floodEventId={}", userReportId, floodEventId);
        floodProcessorClient.approveFloodEvent(floodEventId);
    }

    @Override
    public void rejectReport(UUID userReportId) {
        String floodEventId = resolveFloodEventId(userReportId);
        log.info("[ADMIN-FLOOD-SERVICE] Reject report: userReportId={}, floodEventId={}", userReportId, floodEventId);
        floodProcessorClient.rejectFloodEvent(floodEventId);
    }

    private String resolveFloodEventId(UUID userReportId) {
        UserReport report = userReportRepository.findById(userReportId)
                .orElseThrow(() -> new AppException(CoreErrorCode.USER_REPORT_NOT_FOUND,
                        "Không tìm thấy báo cáo: " + userReportId));

        if (report.getFloodEventId() == null) {
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Báo cáo chưa được xử lý bởi hệ thống, vui lòng thử lại sau");
        }

        return report.getFloodEventId();
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
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
