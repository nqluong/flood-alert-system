package org.project.floodalert.floodcore.mapper;

import org.project.floodalert.floodcore.dto.event.UserReportEvent;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.model.UserReport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;


@Component
public class UserReportMapper {


    public UserReport toEntity(UserReportRequest request, UUID userId, String reportId) {
        return UserReport.builder()
                .reportId(reportId)
                .userId(userId)
                .description(request.getDescription())
                .imageUrls(request.getImageUrl() != null
                        ? request.getImageUrl()
                        : null)
                .severityLevel(request.getLevel() != null
                        ? request.getLevel().name()
                        : null)
                .lat(BigDecimal.valueOf(request.getLat()))
                .lon(BigDecimal.valueOf(request.getLon()))
                .status("PENDING")
                .build();
    }

    public UserReportResponse toResponse(UserReport report) {
        return UserReportResponse.builder()
                .id(report.getId())
                .reportId(report.getReportId())
                .userId(report.getUserId())
                .description(report.getDescription())
                .imageUrls(report.getImageUrls())
                .severityLevel(report.getSeverityLevel())
                .lat(report.getLat() != null ? report.getLat().doubleValue() : null)
                .lon(report.getLon() != null ? report.getLon().doubleValue() : null)
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .message("Báo cáo của bạn đã được ghi nhận và đang chờ xét duyệt.")
                .build();
    }

    public UserReportResponse toSummaryResponse(UserReport report) {
        return UserReportResponse.builder()
                .id(report.getId())
                .reportId(report.getReportId())
                .userId(report.getUserId())
                .description(report.getDescription())
                .imageUrls(report.getImageUrls())
                .severityLevel(report.getSeverityLevel())
                .lat(report.getLat() != null ? report.getLat().doubleValue() : null)
                .lon(report.getLon() != null ? report.getLon().doubleValue() : null)
                .status(report.getStatus())
                .floodEventId(report.getFloodEventId())
                .createdAt(report.getCreatedAt())
                .build();
    }

    public UserReportEvent toEvent(UserReport report) {
        return UserReportEvent.builder()
                .reportId(report.getReportId())
                .userId(report.getUserId())
                .imageUrl(report.getImageUrls())
                .severityLevel(report.getSeverityLevel())
                .lat(report.getLat() != null ? report.getLat().doubleValue() : null)
                .lon(report.getLon() != null ? report.getLon().doubleValue() : null)
                .description(report.getDescription())
                .reportedAt(report.getCreatedAt())
                .build();
    }
}

