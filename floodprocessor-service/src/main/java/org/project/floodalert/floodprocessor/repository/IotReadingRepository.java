package org.project.floodalert.floodprocessor.repository;

import org.project.floodalert.floodprocessor.model.IoTReading;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
