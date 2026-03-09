package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity FloodEvent.
 * Cung cấp các truy vấn đặc thù phục vụ Module 5 (Event Aggregator).
 */
@Repository
public interface FloodEventRepository extends JpaRepository<FloodEvent, UUID> {

    /**
     * Tìm sự kiện ngập đang Active của một sensor cụ thể.
     * Điều kiện: nguồn = 'SENSOR', thuộc sensor đã cho, trạng thái PENDING hoặc CONFIRMED,
     * và chưa hết hạn (expires_at > now).
     *
     * @param sensorId mã định danh của sensor
     * @param now      thời điểm hiện tại để so sánh expires_at
     * @return sự kiện active nếu tồn tại
     */
    @Query("""
            SELECT e FROM FloodEvent e
            WHERE e.source = 'SENSOR'
              AND e.sourceId = :sensorId
              AND e.status IN ('PENDING', 'CONFIRMED')
              AND e.expiresAt > :now
            ORDER BY e.createdAt DESC
            """)
    Optional<FloodEvent> findActiveEventBySensorId(
            @Param("sensorId") String sensorId,
            @Param("now") LocalDateTime now);


    @Query(value = """
            SELECT * FROM flood_events e
            WHERE e.source IN ('SENSOR', 'IOT')
                AND e.status IN ('PENDING', 'CONFIRMED')
            """, nativeQuery = true)
    List<FloodEvent> findActiveIotEvents();

}
