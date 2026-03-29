package org.project.floodalert.floodprocessor.service.admin;

import java.util.UUID;

/**
 * Service xử lý Admin manual approval/rejection cho FloodEvent.
 * Dùng cho trường hợp Admin cần duyệt hoặc từ chối báo cáo ngập lụt thủ công.
 */
public interface AdminApprovalService {

    /**
     * Admin duyệt flood event PENDING thành ACTIVE.
     * <p>
     * Logic:
     * - Update status = ACTIVE
     * - Update confidence_score = 1.0
     * - Update confirmed_at = now
     * - Lấy tất cả contributors (PENDING) qua persistenceService.getContributorsByEventId
     * - Bắn Kafka Reputation Event: PIONEER +5, VERIFIER +2
     * - Bắn Kafka Lifecycle Event: UPDATED
     *
     * @param eventId ID của flood event (eventId, không phải UUID)
     */
    void approveEvent(String eventId);

    /**
     * Admin từ chối flood event PENDING.
     * <p>
     * Logic:
     * - Update status = REJECTED
     * - Lấy tất cả contributors (PENDING) qua persistenceService.getContributorsByEventId
     * - Bắn Kafka Reputation Event: Penalty -10 điểm cho tất cả contributors
     *
     * @param eventId ID của flood event (eventId, không phải UUID)
     */
    void rejectEvent(String eventId);
}
