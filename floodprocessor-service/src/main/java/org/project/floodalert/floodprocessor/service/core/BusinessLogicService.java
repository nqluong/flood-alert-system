package org.project.floodalert.floodprocessor.service.core;

import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;

import java.util.List;

public interface BusinessLogicService {
    void process(List<EnrichedSensorData> enrichedDataList);
}
