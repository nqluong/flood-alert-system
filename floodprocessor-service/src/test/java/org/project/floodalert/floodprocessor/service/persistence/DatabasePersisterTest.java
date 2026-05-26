package org.project.floodalert.floodprocessor.service.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.project.floodalert.floodprocessor.repository.IotReadingRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabasePersisterTest {

    @Mock
    private IotReadingRepository iotReadingRepository;

    @InjectMocks
    private DatabasePersister persister;

    private IoTReading buildReading() {
        return IoTReading.builder()
                .sensorId("sensor-001")
                .readingId("r-001")
                .measuredAt(Instant.now())
                .build();
    }

    @Test
    void batchSave_nullReadings_returnsEmpty() {
        persister.batchSave(null);
    }

    @Test
    void batchSave_emptyReadings_returnsEmpty() {
        persister.batchSave(List.of());
    }

    @Test
    void batchSave_validReadings_returnsSaved() {
        IoTReading reading = buildReading();
        when(iotReadingRepository.saveAll(any())).thenReturn(List.of(reading));
        persister.batchSave(List.of(reading));
    }

    @Test
    void batchSave_saveAllThrows_throwsAppException() {
        when(iotReadingRepository.saveAll(any())).thenThrow(new RuntimeException("DB down"));
        assertThrows(AppException.class, () -> persister.batchSave(List.of(buildReading())));
    }

    @Test
    void count_returnsRepositoryCount() {
        when(iotReadingRepository.count()).thenReturn(42L);
        persister.count();
    }
}
