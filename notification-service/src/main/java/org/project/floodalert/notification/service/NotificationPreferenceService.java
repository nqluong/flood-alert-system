package org.project.floodalert.notification.service;

import org.project.floodalert.notification.dto.request.NotificationPreferenceDTO;
import org.project.floodalert.notification.dto.response.NotificationPreferenceResponse;

import java.util.UUID;

public interface NotificationPreferenceService {


    NotificationPreferenceResponse getPreferences(UUID userId);

    NotificationPreferenceResponse updatePreferences(UUID userId, NotificationPreferenceDTO dto);
}
