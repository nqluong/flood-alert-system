package org.project.floodalert.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.request.NotificationPreferenceDTO;
import org.project.floodalert.notification.dto.response.NotificationPreferenceResponse;
import org.project.floodalert.notification.model.NotificationPreference;
import org.project.floodalert.notification.repository.NotificationPreferenceRepository;
import org.project.floodalert.notification.service.NotificationPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private static final Integer DEFAULT_ALERT_RADIUS = 500;
    private static final Boolean DEFAULT_ENABLED = true;

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        log.debug("Fetching notification preferences for user: {}", userId);

        return notificationPreferenceRepository.findById(userId)
                .map(this::mapToResponse)
                .orElseGet(this::buildDefaultResponse);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, NotificationPreferenceDTO dto) {
        log.debug("Updating notification preferences for user: {}", userId);

        NotificationPreference preference = notificationPreferenceRepository.findById(userId)
                .orElseGet(() -> createNewPreference(userId));

        updatePreferenceFromDTO(preference, dto);

        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        log.debug("Successfully updated notification preferences for user: {}", userId);

        return mapToResponse(saved);
    }

    private NotificationPreference createNewPreference(UUID userId) {
        return NotificationPreference.builder()
                .userId(userId)
                .build();
    }

    private void updatePreferenceFromDTO(NotificationPreference preference, NotificationPreferenceDTO dto) {
        preference.setEnabled(dto.getEnabled());
        preference.setFloodAlerts(dto.getFloodAlerts());
        preference.setQuietHoursEnabled(dto.getQuietHoursEnabled());
        preference.setQuietHoursStart(dto.getQuietHoursStart());
        preference.setQuietHoursEnd(dto.getQuietHoursEnd());
        preference.setAlertRadiusMeters(dto.getAlertRadiusMeters());
        preference.setPreferPush(dto.getPreferPush());
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .enabled(preference.getEnabled())
                .floodAlerts(preference.getFloodAlerts())
                .quietHoursEnabled(preference.getQuietHoursEnabled())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .alertRadiusMeters(preference.getAlertRadiusMeters())
                .preferPush(preference.getPreferPush())
                .build();
    }

    private NotificationPreferenceResponse buildDefaultResponse() {
        log.debug("Building default notification preferences");
        return NotificationPreferenceResponse.builder()
                .enabled(DEFAULT_ENABLED)
                .floodAlerts(true)
                .quietHoursEnabled(false)
                .quietHoursStart(null)
                .quietHoursEnd(null)
                .alertRadiusMeters(DEFAULT_ALERT_RADIUS)
                .preferPush(true)
                .build();
    }
}
