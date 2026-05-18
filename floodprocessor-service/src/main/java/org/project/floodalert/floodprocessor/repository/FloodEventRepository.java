package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.FloodEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FloodEventRepository extends JpaRepository<FloodEvent, UUID> {

    Optional<FloodEvent> findByEventId(String eventId);

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

    /**
     * Tìm tất cả sự kiện ngập đang Active của nhiều sensors (batch query).
     * Tối ưu hóa để tránh N+1 query problem.
     *
     * @param sensorIds danh sách mã định danh của sensors
     * @param now       thời điểm hiện tại để so sánh expires_at
     * @return danh sách sự kiện active
     */
    @Query("""
            SELECT e FROM FloodEvent e
            WHERE e.source = 'SENSOR'
              AND e.sourceId IN :sensorIds
              AND e.status IN ('PENDING', 'CONFIRMED')
              AND e.expiresAt > :now
            ORDER BY e.sourceId, e.createdAt DESC
            """)
    List<FloodEvent> findActiveEventsBySensorIds(
            @Param("sensorIds") List<String> sensorIds,
            @Param("now") LocalDateTime now);


    @Query(value = """
            SELECT * FROM flood_events e
            WHERE e.source IN ('SENSOR', 'IOT')
                AND e.status IN ('PENDING', 'CONFIRMED')
            """, nativeQuery = true)
    List<FloodEvent> findActiveIotEvents();

    /**
     * Tìm các sự kiện ngập từ nguồn IoT/Sensor trong bán kính và khoảng thời gian cho trước.
     * Khoảng cách geodetic tính bằng công thức Haversine (đơn vị: km).
     * Dùng LEAST(1.0, ...) để tránh lỗi acos do rounding-error vượt domain [-1, 1].
     *
     * @param lat      vĩ độ tâm tìm kiếm
     * @param lon      kinh độ tâm tìm kiếm
     * @param radiusKm bán kính tìm kiếm (km)
     * @param since    giới hạn thời gian (chỉ lấy bản ghi có updated_at >= since)
     */
    @Query(value = """
            SELECT * FROM flood_processor.flood_events
            WHERE source IN ('SENSOR', 'IOT')
              AND updated_at >= :since
              AND ST_DistanceSphere(
                    ST_MakePoint(CAST(lon AS double precision), CAST(lat AS double precision)),
                    ST_MakePoint(:lon, :lat)
                  ) <= (:radiusKm * 1000)
            ORDER BY updated_at DESC
            """, nativeQuery = true)
    List<FloodEvent> findRecentIotEventsNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm,
            @Param("since") LocalDateTime since);

    /**
     * Đếm số báo cáo USER_REPORT đang active trong bán kính và khoảng thời gian cho trước.
     * Dùng cho Bước 2 (Crowd Consensus) của SpatialConsensusScoringStrategy.
     *
     * @param lat      vĩ độ tâm tìm kiếm
     * @param lon      kinh độ tâm tìm kiếm
     * @param radiusKm bán kính tìm kiếm (km)
     * @param since    giới hạn thời gian (chỉ lấy bản ghi có created_at >= since)
     */
    @Query(value = """
            SELECT COALESCE(SUM(vote_count), 0) 
            FROM flood_processor.flood_events
            WHERE source = 'USER_REPORT'
              AND status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
              AND created_at >= :since
              AND ST_DistanceSphere(
                    ST_MakePoint(lon::double precision, lat::double precision),
                    ST_MakePoint(:lon, :lat)
                  ) <= (:radiusKm * 1000)
            """, nativeQuery = true)
    int countNearbyActiveUserReports(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm,
            @Param("since") LocalDateTime since);


    @Query("""
            SELECT e FROM FloodEvent e
            WHERE e.source = 'USER_REPORT'
              AND e.status = 'PENDING'
              AND e.createdAt < :cutoffTime
            ORDER BY e.createdAt ASC
            """)
    Page<FloodEvent> findPendingUserReportsOlderThan(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);

    @Query("""
            SELECT e FROM FloodEvent e
            WHERE e.source = 'USER_REPORT'
              AND e.status IN ('ACTIVE', 'CONFIRMED')
              AND e.updatedAt < :cutoffTime
            ORDER BY e.updatedAt ASC
            """)
    Page<FloodEvent> findActiveUserReportsWithoutRecentUpdate(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM flood_processor.flood_events
            WHERE source IN ('SENSOR', 'IOT')
              AND status IN ('PENDING', 'CONFIRMED')
              AND ST_DistanceSphere(
                    ST_MakePoint(lon::double precision, lat::double precision),
                    ST_MakePoint(:lon, :lat)
                  ) <= (:radiusKm * 1000)
            ORDER BY updated_at DESC
            """, nativeQuery = true)
    List<FloodEvent> findActiveSensorEventsNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm);

}
