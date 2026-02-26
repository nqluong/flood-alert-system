package org.project.floodalert.floodcore.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.event.SensorHealthSyncEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorBatchUpdateRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String BATCH_UPDATE_SQL =
            // language=SQL
            "UPDATE flood_core.sensors " +
            "SET battery_level = ?, " +
            "    signal_strength = ?, " +
            "    status = ?, " +
            "    last_heartbeat = to_timestamp(? / 1000.0) " +
            "WHERE sensor_id = ? " +
            "  AND status NOT IN ('MAINTENANCE', 'DISABLED')";


    @SuppressWarnings("UnusedReturnValue")
    public int[] batchUpdateSensorHealth(List<SensorHealthSyncEvent> events) {
        if (events == null || events.isEmpty()) {
            log.warn("[SensorBatchUpdate] Danh sách events trống, bỏ qua batch update.");
            return new int[0];
        }

        log.info("[SensorBatchUpdate] Bắt đầu batch update sức khỏe sensor, kích thước lô: {} bản ghi", events.size());
        long startTime = System.currentTimeMillis();

        List<Object[]> batchArgs = events.stream()
                .map(event -> new Object[]{
                        toBatteryLevel(event.getBattery()),  // battery_level (Integer)
                        event.getSignalStrength(),            // signal_strength
                        event.getStatus(),                    // status
                        event.getTimestamp(),                 // to_timestamp(? / 1000.0)
                        event.getSensorId()                   // WHERE sensor_id = ?
                })
                .toList();

        int[] result = jdbcTemplate.batchUpdate(BATCH_UPDATE_SQL, batchArgs);

        long elapsed = System.currentTimeMillis() - startTime;
        int totalUpdated = sumAffectedRows(result);
        int totalSkipped = events.size() - totalUpdated;
        log.info("[SensorBatchUpdate] Hoàn thành batch update: {}/{} rows affected, {} bị bỏ qua (MAINTENANCE/DISABLED), thời gian: {}ms",
                totalUpdated, events.size(), totalSkipped, elapsed);

        return result;
    }



    private Integer toBatteryLevel(Double battery) {
        if (battery == null) return null;
        return battery.intValue();
    }

    private int sumAffectedRows(int[] rows) {
        int total = 0;
        for (int row : rows) {
            if (row > 0) total += row;
        }
        return total;
    }
}
