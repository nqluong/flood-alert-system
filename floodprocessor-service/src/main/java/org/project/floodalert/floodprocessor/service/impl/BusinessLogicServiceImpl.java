package org.project.floodalert.floodprocessor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodprocessor.dto.response.EnrichedSensorData;
import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.exception.ProcessorErrorCode;
import org.project.floodalert.floodprocessor.service.BusinessLogicService;
import org.project.floodalert.floodprocessor.service.DispatcherService;
import org.project.floodalert.floodprocessor.service.FloodAssessmentService;
import org.project.floodalert.floodprocessor.service.StateChangeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLogicServiceImpl implements BusinessLogicService {

    private final FloodAssessmentService floodAssessmentService;
    private final StateChangeService stateChangeService;
    private final DispatcherService dispatcherService;

    @Override
    public void process(List<EnrichedSensorData> enrichedDataList) {
        if (enrichedDataList == null) {
            log.error("Dữ liệu enriched bị null, không thể xử lý");
            throw new AppException(ProcessorErrorCode.ENRICHED_DATA_NULL);
        }

        if (enrichedDataList.isEmpty()) {
            log.warn("Dữ liệu enriched rỗng, bỏ qua xử lý");
            return;
        }

        log.info("=== BẮT ĐẦU XỬ LÝ BUSINESS LOGIC: {} records", enrichedDataList.size());

        try {
            log.info("Module 2: Đánh giá trạng thái lũ lụt");
            List<ProcessedSensorData> processedDataList =
                    floodAssessmentService.assessFloodStatus(enrichedDataList);
            log.info("Module 2: Hoàn thành - {} records được đánh giá", processedDataList.size());

            log.info("Module 3: Phát hiện thay đổi trạng thái");
            stateChangeService.detectStateChanges(processedDataList);
            log.info("Module 3: Hoàn thành - State changes đã được detect");

            log.info("Module 4: Gửi alerts/notifications (TODO)");
            dispatcherService.dispatch(processedDataList);

            log.info("=== HOÀN THÀNH XỬ LÝ BUSINESS LOGIC: {} records ===",
                    processedDataList.size());

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi xử lý business logic", e);
            throw new AppException(ProcessorErrorCode.PROCESSING_FAILED);
        }
    }
}
