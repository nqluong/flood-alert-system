package org.project.floodalert.floodcore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.floodcore.dto.request.VirtualSensorProvisionRequest;
import org.project.floodalert.floodcore.dto.response.VirtualSensorCleanupResponse;
import org.project.floodalert.floodcore.dto.response.VirtualSensorProvisionResponse;
import org.project.floodalert.floodcore.service.VirtualSensorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/sensors/virtual")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;

    @PostMapping("/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VirtualSensorProvisionResponse>> provisionVirtualSensors(
            @Valid @RequestBody VirtualSensorProvisionRequest request) {

        log.info("API POST /api/v1/sensors/virtual/provision - Target count: {}", 
                request.getTargetCount());

        VirtualSensorProvisionResponse response = virtualSensorService.provisionVirtualSensors(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<VirtualSensorProvisionResponse>builder()
                        .success(true)
                        .code(HttpStatus.CREATED.value())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VirtualSensorCleanupResponse>> cleanupVirtualSensors() {

        log.warn("API DELETE /api/v1/sensors/virtual/clean - Bắt đầu cleanup virtual sensors");

        VirtualSensorCleanupResponse response = virtualSensorService.cleanupVirtualSensors();

        return ResponseEntity.ok(ApiResponse.<VirtualSensorCleanupResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
