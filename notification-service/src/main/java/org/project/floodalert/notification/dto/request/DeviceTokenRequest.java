package org.project.floodalert.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenRequest(
        @NotBlank(message = "FCM token không được để trống")
        String fcmToken
) {
}
