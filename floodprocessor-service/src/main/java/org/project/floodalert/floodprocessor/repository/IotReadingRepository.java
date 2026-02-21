package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.IoTReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface IotReadingRepository extends JpaRepository<IoTReading, UUID> {

    List<IoTReading> findBySensorId(String sensorId);

    List<IoTReading> findByReadingId(String readingId);

    List<IoTReading> findByStatus(String status);

    List<IoTReading> findByMeasuredAtBetween(Instant measuredAt, Instant measuredAt2);

    List<IoTReading> findBySensorIdAndMeasuredAtBetween(String sensorId, Instant measuredAt, Instant measuredAt2);

    /**
     * Cập nhật flood_event_id cho một IoTReading đã lưu (back-linking).
     * Được gọi sau khi FloodEvent đã được persist thành công.
     *
     * @param floodEventId UUID của FloodEvent vừa được xử lý
     * @param readingId    reading_id (String unique) của bản ghi IoTReading cần cập nhật
     */
    @Modifying
    @Query("UPDATE IoTReading r SET r.floodEventId = :floodEventId WHERE r.readingId = :readingId")
    void updateFloodEventIdByReadingId(
            @Param("floodEventId") java.util.UUID floodEventId,
            @Param("readingId") String readingId);
}
