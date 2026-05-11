package org.project.floodalert.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.auth.dto.request.UserLocationRequest;
import org.project.floodalert.auth.service.UserLocationService;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.InternalUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/location")
@RequiredArgsConstructor
public class UserLocationController {

    private final UserLocationService userLocationService;


    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            Authentication authentication,
            @Valid @RequestBody UserLocationRequest request
    ) {
        InternalUserDetails userDetails = (InternalUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(userDetails.getUserId());

        log.info("Received location update from user: {}", userId);

        userLocationService.updateLocation(userId, request.getLat(), request.getLon());

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.NO_CONTENT.value())
                .message("User location updated successfully")
                .build());
    }
}
