package org.project.floodalert.floodprocessor.enums;

public enum ReputationReason {

    /**
     * Báo cáo bị tự động từ chối (score < 0.3).
     * Penalty: -2 điểm
     */
    AUTO_REJECTED,

    /**
     * Báo cáo được cụm xác nhận (PENDING -> ACTIVE).
     * Reward: +5 điểm (PIONEER) hoặc +2 điểm (VERIFIER)
     */
    CLUSTER_APPROVED,

    /**
     * Xác nhận thêm cho event đã ACTIVE.
     * Reward: +2 điểm
     */
    ACTIVE_CONFIRMATION,

    /**
     * Admin duyệt báo cáo PENDING thành ACTIVE.
     * Reward: PIONEER +5 điểm, VERIFIER +2 điểm
     */
    ADMIN_APPROVED,

    /**
     * Admin từ chối báo cáo PENDING.
     * Penalty: -10 điểm (báo cáo giả)
     */
    ADMIN_REJECTED
}
