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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/v1/admin/floods")
@RequiredArgsConstructor
public class AdminFloodController {

    private final AdminFloodService adminFloodService;

    /**
     * API lấy toàn bộ danh sách điểm ngập đang hoạt động.
     * Dùng cho Admin Dashboard khi initial load trang bản đồ.
     *
     */
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
}
