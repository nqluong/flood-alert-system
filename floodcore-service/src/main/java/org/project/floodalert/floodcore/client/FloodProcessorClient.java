package org.project.floodalert.floodcore.client;

import org.project.floodalert.common.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "flood-processor",
        url = "${flood-processor.url:http://localhost:8084}"
)
public interface FloodProcessorClient {

    /**
     * Admin duyệt flood event PENDING thành ACTIVE.
     * Update status, confidence_score = 1.0, confirmed_at = now.
     * Reward PIONEER +5, VERIFIER +2 điểm.
     *
     * @param eventId UUID của flood event cần duyệt
     * @return ApiResponse chứa thông tin event đã approve
     */
    @PostMapping("/internal/api/v1/admin/floods/{eventId}/approve")
    ApiResponse<Void> approveFloodEvent(@PathVariable("eventId") String eventId);

    /**
     * Admin từ chối flood event PENDING.
     * Update status = REJECTED.
     * Penalty tất cả contributors -10 điểm.
     *
     * @param eventId UUID của flood event cần từ chối
     * @return ApiResponse chứa thông tin event đã reject
     */
    @PostMapping("/internal/api/v1/admin/floods/{eventId}/reject")
    ApiResponse<Void> rejectFloodEvent(@PathVariable("eventId") String eventId);
}
