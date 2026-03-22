package org.project.floodalert.auth.service;

import java.util.UUID;

public interface ReputationCacheService {

    /**
     * Cache điểm reputation bất đồng bộ (fire-and-forget).
     * Dùng khi đăng nhập để không chặn luồng chính.
     */
    void cacheAsync(UUID userId, int reputationScore);

    /**
     * Lấy điểm reputation: kiểm tra cache trước, nếu miss thì load từ DB rồi cache lại.
     * Dùng cho API internal khi service khác cần tra cứu.
     */
    int getOrLoad(UUID userId);
}
