package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.response.AdminActiveFloodResponse;
import java.util.List;

public interface AdminFloodService {
    List<AdminActiveFloodResponse> getAllActiveFloods();
}
