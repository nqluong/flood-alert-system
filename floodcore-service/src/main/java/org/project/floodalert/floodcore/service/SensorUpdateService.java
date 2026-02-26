package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.request.ChangeStatusRequest;
import org.project.floodalert.floodcore.dto.request.DeleteSensorRequest;
import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.dto.response.ChangeStatusResponse;
import org.project.floodalert.floodcore.dto.response.DeleteSensorResponse;
import org.project.floodalert.floodcore.dto.response.UpdateSensorResponse;

import java.util.UUID;

public interface SensorUpdateService {
    /**
     * Update sensor theo UUID
     *
     * @param sensorId UUID của sensor cần update
     * @param request Update request chứa các fields cần thay đổi
     * @param performedBy UUID của người thực hiện
     * @return Response chứa thông tin sensor đã update
     */
    UpdateSensorResponse updateSensor(UUID sensorId, UpdateSensorRequest request, UUID performedBy);

    /**
     * Update sensor theo sensorId (string)
     *
     * @param sensorId Mã sensor (VD: SENS-HAN-01)
     * @param request Update request
     * @param performedBy UUID của người thực hiện
     * @return Response chứa thông tin sensor đã update
     */
    UpdateSensorResponse updateSensorBySensorId(String sensorId, UpdateSensorRequest request, UUID performedBy);

    /**
     * Soft Delete
     *
     * Chuyển status thành DISABLED hoặc DELETED
     *
     * @param sensorId UUID của sensor
     * @param request Delete request chứa lý do và options
     * @param performedBy UUID của người thực hiện
     * @return Response chứa thông tin delete
     */
    DeleteSensorResponse softDelete(UUID sensorId, DeleteSensorRequest request, UUID performedBy);

    /**
     * Soft Delete theo sensorId
     */
    DeleteSensorResponse softDeleteBySensorId(String sensorId, DeleteSensorRequest request, UUID performedBy);

    /**
     * Hard Delete
     *
     * @param sensorId UUID của sensor
     * @param request Delete request
     * @param performedBy UUID của người thực hiện
     * @return Response xác nhận đã xóa
     */
    DeleteSensorResponse hardDelete(UUID sensorId, DeleteSensorRequest request, UUID performedBy);

    /**
     * Hard Delete theo sensorId
     */
    DeleteSensorResponse hardDeleteBySensorId(String sensorId, DeleteSensorRequest request, UUID performedBy);

    /**
     * Restore sensor đã bị soft delete
     * Chuyển status từ DISABLED/DELETED về ACTIVE
     *
     * @param sensorId UUID của sensor
     * @param performedBy UUID của người thực hiện
     * @return Response xác nhận đã restore
     */
    DeleteSensorResponse restoreSensor(UUID sensorId, UUID performedBy);

    /**
     * Restore sensor theo sensorId
     */
    DeleteSensorResponse restoreSensorBySensorId(String sensorId, UUID performedBy);

    /**
     * Chuyển đổi trạng thái (device status) của sensor theo UUID.
     * Tuân thủ ma trận chuyển đổi hợp lệ, ghi audit log và đồng bộ Redis.
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
