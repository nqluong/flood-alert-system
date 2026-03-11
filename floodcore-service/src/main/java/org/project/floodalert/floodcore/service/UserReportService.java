package org.project.floodalert.floodcore.service;

import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;

import java.util.UUID;

public interface UserReportService {

    UserReportResponse submitUserReport(UserReportRequest request, UUID userId);

    PageResponse<UserReportResponse> getReportsByUser(UUID userId, UserReportFilterRequest filter);

    PageResponse<UserReportResponse> getAllReports(UserReportFilterRequest filter);
}
