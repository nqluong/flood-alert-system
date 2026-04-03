package org.project.floodalert.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.notification.dto.request.DeviceTokenRequest;
import org.project.floodalert.notification.service.DeviceTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/device-token")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateToken(@RequestBody @Valid DeviceTokenRequest request){
        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        deviceTokenService.saveToken(userId, request.fcmToken());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Device token updated successfully")
                .build());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeToken() {
        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        deviceTokenService.removeToken(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Device token deleted successfully")
                .build());
    }
}
