package org.project.floodalert.ingestion.service;

import org.project.floodalert.ingestion.domain.SensorMessage;
import org.project.floodalert.ingestion.dto.DeviceInfo;
import org.project.floodalert.ingestion.dto.SensorDataDTO;
import org.project.floodalert.ingestion.validation.ValidationContext;
import org.springframework.stereotype.Component;


@Component
public class CleanDTOBuilder {
    

    public SensorMessage buildCleanMessage(ValidationContext context) {
        SensorDataDTO originalData = context.getSensorData();
        
        SensorDataDTO cleanData = SensorDataDTO.builder()
                .deviceInfo(DeviceInfo.builder()
                        .sensorId(originalData.getDeviceInfo().getSensorId())
                        .firmwareVer(originalData.getDeviceInfo().getFirmwareVer())
                        .model(originalData.getDeviceInfo().getModel())
                        .build())
                .telemetry(originalData.getTelemetry())
                .health(originalData.getHealth())
                .timestamp(originalData.getTimestamp())
                .build();
        
        return SensorMessage.builder()
                .sensorId(context.getSensorId())
                .sensorData(cleanData)
                .topic(context.getTopic())
                .receivedAt(context.getReceivedAt())
                .build();
    }
}
