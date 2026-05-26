package org.project.floodalert.floodprocessor.service.core.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.mapper.IotReadingMapper;
import org.project.floodalert.floodprocessor.messaging.publisher.KafkaDispatcher;
import org.project.floodalert.floodprocessor.model.IoTReading;
import org.project.floodalert.floodprocessor.service.persistence.DatabasePersister;
import org.project.floodalert.floodprocessor.service.processing.SensorHealthThrottlingService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatcherServiceImplTest {

    @Mock private IotReadingMapper iotReadingMapper;
    @Mock private DatabasePersister databasePersister;
    @Mock private KafkaDispatcher kafkaDispatcher;
    @Mock private SensorHealthThrottlingService sensorHealthThrottlingService;

    @InjectMocks
    private DispatcherServiceImpl service;

    // null input → return early
    @Test
    void dispatch_nullInput_returnsEarly() {
        service.dispatch(null);
        verifyNoInteractions(iotReadingMapper, databasePersister, kafkaDispatcher, sensorHealthThrottlingService);
    }

    @Test
    void dispatch_emptyInput_returnsEarly() {
        service.dispatch(Collections.emptyList());
        verifyNoInteractions(iotReadingMapper, databasePersister, kafkaDispatcher, sensorHealthThrottlingService);
    }

    @Test
    void dispatch_kafkaSendThrows_logsErrorAndContinues() {
        ProcessedSensorData data = mockData("s1");
        when(iotReadingMapper.toEntities(any())).thenReturn(List.of(new IoTReading()));
        doThrow(new RuntimeException("kafka error")).when(kafkaDispatcher).send(data);

        service.dispatch(List.of(data));

        verify(databasePersister).batchSave(any());
        verify(sensorHealthThrottlingService).checkAndSend(data);
    }

    @Test
    void dispatch_partialKafkaFailure_logsWarn() {
        ProcessedSensorData ok = mockData("s1");
        ProcessedSensorData fail = mockData("s2");
        when(iotReadingMapper.toEntities(any())).thenReturn(List.of(new IoTReading(), new IoTReading()));
        doThrow(new RuntimeException("fail")).when(kafkaDispatcher).send(fail);

        service.dispatch(List.of(ok, fail));

        verify(kafkaDispatcher).send(ok);
        verify(kafkaDispatcher).send(fail);
    }

    @Test
    void dispatch_healthSyncThrows_logsErrorAndContinues() {
        ProcessedSensorData data = mockData("s1");
        when(iotReadingMapper.toEntities(any())).thenReturn(List.of(new IoTReading()));
        doThrow(new RuntimeException("health error")).when(sensorHealthThrottlingService).checkAndSend(data);

        service.dispatch(List.of(data));

        verify(kafkaDispatcher).send(data);
    }

    private ProcessedSensorData mockData(String sensorId) {
        ProcessedSensorData data = mock(ProcessedSensorData.class);
        when(data.getSensorId()).thenReturn(sensorId);
        return data;
    }
}