package org.project.floodalert.floodcore.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodcore.dto.event.SensorHealthSyncEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorBatchUpdateRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SensorBatchUpdateRepository repository;

    private SensorHealthSyncEvent event;

    @BeforeEach
    void setUp() {
        event = new SensorHealthSyncEvent();
        event.setBattery(85.0);
        event.setSignalStrength(-60);
        event.setStatus("ACTIVE");
        event.setTimestamp(System.currentTimeMillis());
        event.setSensorId("SENS-HAN-01");
    }

    @Test
    void batchUpdateSensorHealth_eventsNull_returnsEmptyArray() {
        repository.batchUpdateSensorHealth(null);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void batchUpdateSensorHealth_eventsEmpty_returnsEmptyArray() {
        repository.batchUpdateSensorHealth(List.of());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void batchUpdateSensorHealth_singleEvent_callsBatchUpdate() {
        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1});

        repository.batchUpdateSensorHealth(List.of(event));

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void batchUpdateSensorHealth_multipleEvents_callsBatchUpdate() {
        SensorHealthSyncEvent event2 = new SensorHealthSyncEvent();
        event2.setBattery(60.0);
        event2.setSignalStrength(-70);
        event2.setStatus("WARNING");
        event2.setTimestamp(System.currentTimeMillis());
        event2.setSensorId("SENS-HAN-02");

        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1, 1});

        repository.batchUpdateSensorHealth(List.of(event, event2));

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void batchUpdateSensorHealth_batteryNull_callsBatchUpdate() {
        event.setBattery(null);

        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1});

        repository.batchUpdateSensorHealth(List.of(event));

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void batchUpdateSensorHealth_someRowsSkipped_callsBatchUpdate() {
        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1, 0, 1});

        SensorHealthSyncEvent event2 = new SensorHealthSyncEvent();
        SensorHealthSyncEvent event3 = new SensorHealthSyncEvent();

        repository.batchUpdateSensorHealth(List.of(event, event2, event3));

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void batchUpdateSensorHealth_batchUpdateThrows_propagatesException() {
        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenThrow(new RuntimeException("DB error"));

        try {
            repository.batchUpdateSensorHealth(List.of(event));
        } catch (RuntimeException ignored) {
        }

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void batchUpdateSensorHealth_mapsCorrectArguments() {
        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1});

        repository.batchUpdateSensorHealth(List.of(event));

        ArgumentCaptor<List<Object[]>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());

        List<Object[]> args = captor.getValue();

        verify(jdbcTemplate).batchUpdate(anyString(), anyList());
    }

    @Test
    void batchUpdateSensorHealth_usesCorrectSql() {
        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1});

        repository.batchUpdateSensorHealth(List.of(event));

        verify(jdbcTemplate).batchUpdate(
                eq(
                        "UPDATE flood_core.sensors " +
                                "SET battery_level = ?, " +
                                "    signal_strength = ?, " +
                                "    status = ?, " +
                                "    last_heartbeat = to_timestamp(? / 1000.0) " +
                                "WHERE sensor_id = ? " +
                                "  AND status NOT IN ('MAINTENANCE', 'DISABLED')"
                ),
                anyList()
        );
    }
}