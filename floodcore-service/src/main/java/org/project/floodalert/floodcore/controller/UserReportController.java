package org.project.floodalert.floodcore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.request.UserReportFilterRequest;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.service.UserReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;

    /**
     * Người dùng gửi báo cáo ngập lụt.
     * POST /api/v1/reports
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserReportResponse>> submitReport(
            @Valid @RequestBody UserReportRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("POST /api/v1/reports - userId={}", userId);

        UserReportResponse response = userReportService.submitUserReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Người dùng xem danh sách báo cáo của chính mình, có filter và phân trang.
     * GET /api/v1/reports/my?page=0&size=20&status=PENDING&severityLevel=HIGH
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<UserReportResponse>>> getMyReports(
            @Valid @ModelAttribute UserReportFilterRequest filter) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("GET /api/v1/reports/my - userId={}, filter={}", userId, filter);

        PageResponse<UserReportResponse> response = userReportService.getReportsByUser(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Admin xem tất cả báo cáo, có filter và phân trang.
     * GET /api/v1/reports/admin?page=0&size=20&status=PENDING&severityLevel=CRITICAL
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserReportResponse>>> getAllReports(
            @Valid @ModelAttribute UserReportFilterRequest filter) {

        log.info("GET /api/v1/reports/admin - filter={}", filter);

        PageResponse<UserReportResponse> response = userReportService.getAllReports(filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Admin xem báo cáo của một người dùng cụ thể, có filter và phân trang.
     * GET /api/v1/reports/admin/users/{userId}?page=0&size=20&status=APPROVED
     */
    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserReportResponse>>> getReportsByUser(
            @PathVariable UUID userId,
            @Valid @ModelAttribute UserReportFilterRequest filter) {

        log.info("GET /api/v1/reports/admin/users/{} - filter={}", userId, filter);

        PageResponse<UserReportResponse> response = userReportService.getReportsByUser(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
