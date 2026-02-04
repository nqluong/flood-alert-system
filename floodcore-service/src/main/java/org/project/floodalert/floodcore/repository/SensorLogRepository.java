package org.project.floodalert.floodcore.repository;

import org.project.floodalert.floodcore.model.SensorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, UUID> {
    // Lấy log theo sensor ID, sắp xếp theo thời gian mới nhất
    List<SensorLog> findBySensorIdOrderByCreatedAtDesc(UUID sensorId);

    // Lấy log theo sensor ID với phân trang
    Page<SensorLog> findBySensorIdOrderByCreatedAtDesc(UUID sensorId, Pageable pageable);

    // Lấy N log gần nhất
    List<SensorLog> findTop10BySensorIdOrderByCreatedAtDesc(UUID sensorId);
}
