package org.project.floodalert.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.notification.dto.request.NotificationPreferenceDTO;
import org.project.floodalert.notification.dto.response.NotificationPreferenceResponse;
import org.project.floodalert.notification.service.NotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences() {
        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        NotificationPreferenceResponse response = notificationPreferenceService.getPreferences(userId);

        return ResponseEntity.ok(ApiResponse.<NotificationPreferenceResponse>builder()
                .success(true)
                .message("Notification preferences retrieved successfully")
                .data(response)
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @RequestBody @Valid NotificationPreferenceDTO dto) {
        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        NotificationPreferenceResponse response = notificationPreferenceService.updatePreferences(userId, dto);

        return ResponseEntity.ok(ApiResponse.<NotificationPreferenceResponse>builder()
                .success(true)
                .message("Notification preferences updated successfully")
                .data(response)
                .build());
    }
}
