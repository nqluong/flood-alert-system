package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.service.BusinessLogicService;
import org.project.floodalert.floodprocessor.service.FloodAssessmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLogicServiceImpl implements BusinessLogicService {

    private final FloodAssessmentService floodAssessmentService;

    @Override
    public void process(List<EnrichedSensorData> enrichedDataList) {
        List<ProcessedSensorData> processedList = floodAssessmentService.assessFloodStatus(enrichedDataList);


    }
}
