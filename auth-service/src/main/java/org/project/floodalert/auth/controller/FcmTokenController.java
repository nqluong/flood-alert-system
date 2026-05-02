package org.project.floodalert.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.floodalert.auth.dto.request.FcmTokenRequest;
import org.project.floodalert.auth.service.FcmTokenService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.InternalUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fcm-tokens")
@RequiredArgsConstructor
public class FcmTokenController {
    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> upsertToken(Authentication authentication,
                                                         @RequestBody @Valid FcmTokenRequest request) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        fcmTokenService.upsertToken(userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("FCM token upserted successfully")
                .build());
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deactivateToken(Authentication authentication,
                                                             @PathVariable(name = "deviceId") String deviceId){
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        fcmTokenService.deactivateToken(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("FCM token deactivated successfully")
                .build());
    }

    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<Void>> deactivateAllTokens(Authentication authentication) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());
        fcmTokenService.deactivateAllTokens(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("All FCM tokens deactivated successfully")
                .build());
    }
}
