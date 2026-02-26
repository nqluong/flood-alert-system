package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.request.ChangeStatusRequest;
import org.project.floodalert.floodcore.dto.response.ChangeStatusResponse;

import java.util.UUID;

/**
 * <p>Các trạng thái hợp lệ: ACTIVE, DISABLED, MAINTENANCE, OFFLINE</p>
 * <p>Ma trận chuyển đổi cho phép:
 * <ul>
 *   <li>ACTIVE      → DISABLED, MAINTENANCE, OFFLINE</li>
 *   <li>DISABLED    → ACTIVE, MAINTENANCE</li>
 *   <li>MAINTENANCE → ACTIVE, DISABLED, OFFLINE</li>
 *   <li>OFFLINE     → ACTIVE, MAINTENANCE</li>
 *   <li>DELETED     → (không cho phép chuyển)</li>
 * </ul>
 * </p>
 */
public interface SensorStatusTransitionService {

    /**
     * Chuyển đổi trạng thái sensor theo UUID.
     *
     * @param sensorId    UUID của sensor
     * @param request     Request chứa trạng thái mới và lý do
     * @param performedBy UUID người thực hiện
     * @return Response chứa thông tin sau khi chuyển trạng thái
     */
    ChangeStatusResponse changeStatus(UUID sensorId, ChangeStatusRequest request, UUID performedBy);

    /**
     * Chuyển đổi trạng thái sensor theo sensorId (mã chuỗi).
     *
     * @param sensorId    Mã sensor (VD: SENS-HAN-01)
     * @param request     Request chứa trạng thái mới và lý do
     * @param performedBy UUID người thực hiện
     * @return Response chứa thông tin sau khi chuyển trạng thái
     */
    ChangeStatusResponse changeStatusBySensorId(String sensorId, ChangeStatusRequest request, UUID performedBy);
}
