package org.project.floodalert.floodcore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.floodcore.dto.response.AdminActiveFloodResponse;
import org.project.floodalert.floodcore.service.AdminFloodService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api/v1/admin/floods")
@RequiredArgsConstructor
public class AdminFloodController {

    private final AdminFloodService adminFloodService;

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminActiveFloodResponse>>> getActiveFloods() {
        List<AdminActiveFloodResponse> activeFloods = adminFloodService.getAllActiveFloods();
        return ResponseEntity.ok(ApiResponse.<List<AdminActiveFloodResponse>>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .data(activeFloods)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Admin phê duyệt một user report.
     * Nhận userReportId (UUID pk của user_reports), tự resolve sang floodEventId,
     * rồi forward sang floodprocessor để approve flood event.
     */
    @PostMapping("/{userReportId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveFloodEvent(@PathVariable UUID userReportId) {
        log.info("[ADMIN-FLOOD-CONTROLLER] Admin phê duyệt report: userReportId={}", userReportId);

        adminFloodService.approveReport(userReportId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Flood event approved successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Admin từ chối một user report.
     * Nhận userReportId (UUID pk của user_reports), tự resolve sang floodEventId,
     * rồi forward sang floodprocessor để reject flood event.
     */
    @PostMapping("/{userReportId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectFloodEvent(@PathVariable UUID userReportId) {
        log.info("[ADMIN-FLOOD-CONTROLLER] Admin từ chối report: userReportId={}", userReportId);

        adminFloodService.rejectReport(userReportId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Flood event rejected successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}

