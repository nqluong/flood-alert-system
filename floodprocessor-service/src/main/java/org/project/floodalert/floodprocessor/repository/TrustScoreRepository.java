package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.TrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrustScoreRepository extends JpaRepository<TrustScore, UUID> {

    /**
     * Lấy tất cả các bản ghi TrustScore của một FloodEvent.
     * Sắp xếp theo thời gian tính toán mới nhất trước.
     *
     * @param floodEventId ID của FloodEvent
     * @return Danh sách TrustScore này, hoặc empty list nếu không có
     */
    @Query("""
            SELECT t FROM TrustScore t
            WHERE t.floodEventId = :floodEventId
            ORDER BY t.calculatedAt DESC
            """)
    List<TrustScore> findByFloodEventIdOrderByCalculatedAtDesc(@Param("floodEventId") UUID floodEventId);

    /**
     * Lấy bản ghi TrustScore gần nhất (mới nhất) của một FloodEvent.
     *
     * @param floodEventId ID của FloodEvent
     * @return TrustScore gần nhất, hoặc empty nếu không có
     */
    @Query("""
            SELECT t FROM TrustScore t
            WHERE t.floodEventId = :floodEventId
            ORDER BY t.calculatedAt DESC
            LIMIT 1
            """)
    TrustScore findLatestByFloodEventId(@Param("floodEventId") UUID floodEventId);
}
