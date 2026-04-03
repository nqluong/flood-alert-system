package org.project.floodalert.floodprocessor.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.floodprocessor.service.admin.AdminApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


@Slf4j
@RestController
@RequestMapping("/internal/api/v1/admin/floods")
@RequiredArgsConstructor
public class InternalAdminFloodController {

    private final AdminApprovalService adminApprovalService;

    /**
     * Internal API: Admin duyệt flood event PENDING thành ACTIVE.
     * <p>
     * Logic:
     * - Update status = ACTIVE, confidence_score = 1.0, confirmed_at = now
     * - Reward PIONEER +5, VERIFIER +2 điểm
     * - Bắn Lifecycle Event UPDATED
     *
     * @param eventId UUID của flood event cần duyệt
     * @return ApiResponse xác nhận việc duyệt thành công
     */
    @PostMapping("/{eventId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveFloodEvent(@PathVariable String eventId) {
        log.info("[INTERNAL-ADMIN-CONTROLLER] Nhận request duyệt flood event: eventId={}", eventId);

        adminApprovalService.approveEvent(eventId);

        log.info("[INTERNAL-ADMIN-CONTROLLER] Đã duyệt thành công flood event: eventId={}", eventId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Flood event approved successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Internal API: Admin từ chối flood event PENDING.
     * <p>
     * Logic:
     * - Update status = REJECTED
     * - Penalty tất cả contributors -10 điểm
     *
     * @param eventId UUID của flood event cần từ chối
     * @return ApiResponse xác nhận việc từ chối thành công
     */
    @PostMapping("/{eventId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFloodEvent(@PathVariable String eventId) {
        log.info("[INTERNAL-ADMIN-CONTROLLER] Nhận request từ chối flood event: eventId={}", eventId);

        adminApprovalService.rejectEvent(eventId);

        log.info("[INTERNAL-ADMIN-CONTROLLER] Đã từ chối thành công flood event: eventId={}", eventId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Flood event rejected successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
