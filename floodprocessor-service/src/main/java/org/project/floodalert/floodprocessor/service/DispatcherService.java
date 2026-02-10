package org.project.floodalert.floodprocessor.service;

import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;

import java.util.List;

public interface DispatcherService {
    void dispatch(List<ProcessedSensorData> processedSensorDataList);
}
