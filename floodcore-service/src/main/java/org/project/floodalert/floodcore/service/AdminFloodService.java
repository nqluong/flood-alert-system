package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.response.AdminActiveFloodResponse;
import java.util.List;
import java.util.UUID;

public interface AdminFloodService {
    List<AdminActiveFloodResponse> getAllActiveFloods();
    void approveReport(UUID userReportId);
    void rejectReport(UUID userReportId);
    void dismissFlood(String eventId);
}
