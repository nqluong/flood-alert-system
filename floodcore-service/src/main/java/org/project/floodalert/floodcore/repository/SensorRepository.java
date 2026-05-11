package org.project.floodalert.floodcore.repository;

import org.project.floodalert.floodcore.model.Sensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    boolean existsBySensorId(String sensorId);

    Optional<Sensor> findBySensorId(String sensorId);

    boolean existsByApiKey(String apiKey);

    // Tìm theo status
    Page<Sensor> findByStatus(String status, Pageable pageable);

    // Tìm kiếm theo tên hoặc sensor ID
    @Query("SELECT s FROM Sensor s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.sensorId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Sensor> searchByNameOrSensorId(@Param("search") String search, Pageable pageable);

    // Tìm theo status và search
    @Query("SELECT s FROM Sensor s WHERE " +
            "s.status = :status AND " +
            "(LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.sensorId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Sensor> findByStatusAndSearch(@Param("status") String status,
                                       @Param("search") String search,
                                       Pageable pageable);

    // Lấy tất cả sensors cho map (chỉ cần một số field)
    @Query("SELECT s FROM Sensor s WHERE s.status IN ('ACTIVE', 'MAINTENANCE')")
    List<Sensor> findAllActiveSensors();

    // Lấy sensors theo danh sách IDs
    List<Sensor> findByIdIn(List<UUID> ids);

    // Virtual sensor queries
    long countByIsVirtual(Boolean isVirtual);
    
    List<Sensor> findByIsVirtual(Boolean isVirtual);
    
    @Query("SELECT s.sensorId FROM Sensor s WHERE s.isVirtual = true ORDER BY s.sensorId DESC")
    List<String> findAllVirtualSensorIds();
}
