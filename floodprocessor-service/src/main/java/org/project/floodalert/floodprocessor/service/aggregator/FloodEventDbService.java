package org.project.floodalert.floodprocessor.service.aggregator;

import org.project.floodalert.floodprocessor.dto.response.ProcessedSensorData;
import org.project.floodalert.floodprocessor.enums.LifecycleEventType;
import org.project.floodalert.floodprocessor.model.FloodEvent;

import java.util.List;

/**
 * - Tra cứu sự kiện active của sensor
 * - Xử lý 3 kịch bản: Nước rút (A), Ngập mới (B), Ngập kéo dài (C)
 * - Lưu FloodEvent vào database
 * - Back-link IoTReading với FloodEvent vừa xử lý
 */
public interface FloodEventDbService {

    /**
     * Xử lý toàn bộ logic DB và back-linking cho một bản ghi sensor.
     * Phân tích dữ liệu, áp dụng kịch bản phù hợp, lưu DB, và trả về kết quả để
     * các service tiếp theo (Cache, Lifecycle) hành động.
     *
     * @param data dữ liệu sensor đã qua xử lý (từ Kafka)
     * @return kết quả gồm FloodEvent đã lưu, loại lifecycle event, và cờ có nên publish không
     */
    FloodEventDbResult processAndSave(ProcessedSensorData data);

    /**
     * Phiên bản batch-optimized của {@link #processAndSave}: xử lý toàn bộ danh sách sensor
     * trong một transaction, với 1 query đọc active events và 1 lần saveAll.
     *
     * @param dataList danh sách dữ liệu sensor đã qua xử lý
     * @return danh sách kết quả theo thứ tự tương ứng với dataList
     */
    List<FloodEventDbResult> processAndSaveBatch(List<ProcessedSensorData> dataList);

    /**
     * Record chứa kết quả sau khi xử lý DB.
     *
     * @param floodEvent          thực thể FloodEvent đã được persist (hoặc null nếu trạng thái SAFE và không có event active)
     * @param lifecycleEventType  loại vòng đời được xác định (CREATED, ESCALATED, RESOLVED, hoặc null)
     * @param shouldPublish       true nếu cần bắn Lifecycle Event ra Kafka
     */
    record FloodEventDbResult(
            FloodEvent floodEvent,
            LifecycleEventType lifecycleEventType,
            boolean shouldPublish,
            Double waterLevelSnapshot,
            String severityLevelSnapshot
    ) {
        public static FloodEventDbResult noAction() {
            return new FloodEventDbResult(null, null, false, null, null);
        }
    }
}
