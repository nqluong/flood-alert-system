package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.IoTReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IotReadingRepository extends JpaRepository<IoTReading, UUID> {


    List<IoTReading> findBySensorIdAndMeasuredAtBetween(String sensorId, Instant measuredAt, Instant measuredAt2);

    @Modifying
    @Query("UPDATE IoTReading r SET r.floodEventId = :floodEventId WHERE r.readingId = :readingId")
    void updateFloodEventIdByReadingId(
            @Param("floodEventId") java.util.UUID floodEventId,
            @Param("readingId") String readingId);

    @Query("SELECT r FROM IoTReading r WHERE r.readingId IN :readingIds")
    List<IoTReading> findAllByReadingIds(@Param("readingIds") List<String> readingIds);
}
