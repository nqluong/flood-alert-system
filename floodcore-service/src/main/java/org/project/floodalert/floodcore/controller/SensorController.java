package org.project.floodalert.floodcore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.dto.response.PageResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.floodalert.floodcore.dto.request.CreateSensorRequest;
import org.project.floodalert.floodcore.dto.request.BatchCreateSensorRequest;
import org.project.floodalert.floodcore.dto.request.ChangeStatusRequest;
import org.project.floodalert.floodcore.dto.request.DeleteSensorRequest;
import org.project.floodalert.floodcore.dto.request.SensorFilterRequest;
import org.project.floodalert.floodcore.dto.request.UpdateSensorRequest;
import org.project.floodalert.floodcore.dto.response.*;
import org.project.floodalert.floodcore.service.SensorService;
import org.project.floodalert.floodcore.service.SensorUpdateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {
    private final SensorService sensorService;
    private final SensorUpdateService sensorUpdateService;

    /**
     * API tạo mới sensor
     *
     * @param request Thông tin sensor cần tạo
     * @return Thông tin sensor vừa tạo kèm API Key
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreateSensorResponse>> createSensor(
            @Valid @RequestBody CreateSensorRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("User ID thực hiện: {}", userId);
        log.info("API POST /api/v1/sensors - Tạo sensor mới: {}", request.getSensorId());

        CreateSensorResponse response = sensorService.createSensor(request, userId);

        log.info("API POST /api/v1/sensors - Tạo sensor thành công: {}",
                response.getSensorId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * API tạo nhiều sensor cùng lúc
     *
     * @param request Danh sách sensor cần tạo
     * @return Kết quả tạo từng sensor (thành công/thất bại)
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BatchCreateSensorResponse>> batchCreateSensors(
            @Valid @RequestBody BatchCreateSensorRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("User ID thực hiện: {}", userId);
        log.info("API POST /api/v1/sensors/batch - Tạo batch {} sensors", request.getSensors().size());

        BatchCreateSensorResponse response = sensorService.batchCreateSensors(request, userId);

        log.info("API POST /api/v1/sensors/batch - Hoàn thành: {} thành công, {} thất bại",
                response.getSuccessCount(), response.getFailureCount());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * API lấy danh sách sensors với phân trang và filter
     * <p>
     * GET /api/v1/sensors?page=0&size=20&status=ACTIVE&search=CAU_GIAY
     *
     * @param filter Filter parameters
     * @return Danh sách sensors với phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SensorSummaryResponse>>> getSensorList(
            @Valid @ModelAttribute SensorFilterRequest filter) {

        log.info("API GET /api/v1/sensors - Lấy danh sách sensors: page={}, size={}, status={}, search={}",
                filter.getPage(), filter.getSize(), filter.getStatus(), filter.getSearch());

        PageResponse<SensorSummaryResponse> response = sensorService.getSensorList(filter);

        log.info("API GET /api/v1/sensors - Trả về {} sensors", response.getContent().size());

        return ResponseEntity.ok(ApiResponse.<PageResponse<SensorSummaryResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API lấy chi tiết sensor theo UUID
     * <p>
     * GET /api/v1/sensors/{id}?includeLogs=true
     *
     * @param id          UUID của sensor
     * @param includeLogs Có lấy kèm lịch sử logs không (mặc định: false)
     * @return Chi tiết đầy đủ của sensor
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SensorDetailResponse>> getSensorDetail(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "includeLogs", defaultValue = "false") boolean includeLogs) {

        log.info("API GET /api/v1/sensors/{} - Lấy chi tiết sensor, includeLogs={}", id, includeLogs);

        SensorDetailResponse response = sensorService.getSensorDetail(id, includeLogs);

        return ResponseEntity.ok(ApiResponse.<SensorDetailResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API lấy chi tiết sensor theo sensorId (string)
     * <p>
     * GET /api/v1/sensors/by-sensor-id/SENS-HAN-01?includeLogs=false
     *
     * @param sensorId    Mã sensor (VD: SENS-HAN-01)
     * @param includeLogs Có lấy kèm lịch sử logs không
     * @return Chi tiết đầy đủ của sensor
     */
    @GetMapping("/by-sensor-id/{sensorId}")
    public ResponseEntity<ApiResponse<SensorDetailResponse>> getSensorDetailBySensorId(
            @PathVariable(name = "sensorId") String sensorId,
            @RequestParam(name = "includeLogs", defaultValue = "false") boolean includeLogs) {

        log.info("API GET /api/v1/sensors/by-sensor-id/{} - Lấy chi tiết sensor", sensorId);

        SensorDetailResponse response = sensorService.getSensorDetailBySensorId(sensorId, includeLogs);

        return ResponseEntity.ok(ApiResponse.<SensorDetailResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API lấy dữ liệu GeoJSON cho bản đồ (tất cả sensors)
     * <p>
     * GET /api/v1/sensors/map
     *
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<SensorMapResponse>> getSensorMapData() {

        log.info("API GET /api/v1/sensors/map - Lấy map data cho tất cả sensors");

        SensorMapResponse response = sensorService.getSensorMapData();

        return ResponseEntity.ok(ApiResponse.<SensorMapResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API lấy dữ liệu GeoJSON cho sensors đang hoạt động
     * <p>
     * GET /api/v1/sensors/map/active
     *
     * @return GeoJSON FeatureCollection chỉ gồm sensors ACTIVE và MAINTENANCE
     */
    @GetMapping("/map/active")
    public ResponseEntity<ApiResponse<SensorMapResponse>> getActiveSensorMapData() {

        log.info("API GET /api/v1/sensors/map/active - Lấy map data cho active sensors");

        SensorMapResponse response = sensorService.getActiveSensorMapData();

        return ResponseEntity.ok(ApiResponse.<SensorMapResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API cập nhật sensor theo UUID
     * <p>
     * PUT /api/v1/sensors/{id}
     *
     * @param id      UUID của sensor
     * @param request Thông tin cần cập nhật
     * @return Thông tin sensor sau khi cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UpdateSensorResponse>> updateSensor(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateSensorRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("API PUT /api/v1/sensors/{} - Cập nhật sensor bởi user: {}", id, userId);

        UpdateSensorResponse response = sensorUpdateService.updateSensor(id, request, userId);

        log.info("API PUT /api/v1/sensors/{} - Update thành công. Changed: {}",
                id, response.getChangedFields());

        return ResponseEntity.ok(ApiResponse.<UpdateSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API cập nhật sensor theo sensorId
     *
     * @param sensorId Mã sensor (VD: SENS-HAN-01)
     * @param request  Thông tin cần cập nhật
     * @return Thông tin sensor sau khi cập nhật
     */
    @PutMapping("/by-sensor-id/{sensorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UpdateSensorResponse>> updateSensorBySensorId(
            @PathVariable(name = "sensorId") String sensorId,
            @Valid @RequestBody UpdateSensorRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("API PUT /api/v1/sensors/by-sensor-id/{} - Cập nhật sensor bởi user: {}", sensorId, userId);

        UpdateSensorResponse response = sensorUpdateService.updateSensorBySensorId(
                sensorId, request, userId);

        return ResponseEntity.ok(ApiResponse.<UpdateSensorResponse>builder()
                        .success(true)
                        .code(HttpStatus.OK.value())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * API Soft Delete - Vô hiệu hóa sensor
     *
     *
     * @param id UUID của sensor
     * @param request Delete request chứa lý do
     * @return Response xác nhận đã vô hiệu hóa
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DeleteSensorResponse>> softDeleteSensor(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody DeleteSensorRequest request) {

        log.info("API DELETE /api/v1/sensors/{} - Soft delete sensor", id);
        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        DeleteSensorResponse response = sensorUpdateService.softDelete(id, request, userId);

        return ResponseEntity.ok(ApiResponse.<DeleteSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API Soft Delete theo sensorId
     * DELETE /api/v1/sensors/by-sensor-id/{sensorId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/by-sensor-id/{sensorId}")
    public ResponseEntity<ApiResponse<DeleteSensorResponse>> softDeleteBySensorId(
            @PathVariable(name = "sensorId") String sensorId,
            @Valid @RequestBody DeleteSensorRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        DeleteSensorResponse response = sensorUpdateService.softDeleteBySensorId(
                sensorId, request, userId);

        return ResponseEntity.ok(ApiResponse.<DeleteSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API Hard Delete - XÓA VĨNH VIỄN
     *
     * @param id UUID của sensor
     * @param request Delete request với lý do bắt buộc
     * @return Response xác nhận đã xóa vĩnh viễn
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<DeleteSensorResponse>> hardDeleteSensor(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody DeleteSensorRequest request) {

        log.warn("API DELETE /api/v1/sensors/{}/permanent - HARD DELETE REQUEST", id);

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        DeleteSensorResponse response = sensorUpdateService.hardDelete(id, request, userId);

        log.warn("API DELETE /api/v1/sensors/{}/permanent - HARD DELETE COMPLETED", id);

        return ResponseEntity.ok(ApiResponse.<DeleteSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * API Hard Delete theo sensorId
     *
     * DELETE /api/v1/sensors/by-sensor-id/{sensorId}/permanent
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/by-sensor-id/{sensorId}/permanent")
    public ResponseEntity<ApiResponse<DeleteSensorResponse>> hardDeleteBySensorId(
            @PathVariable(name = "sensorId") String sensorId,
            @Valid @RequestBody DeleteSensorRequest request) {

        log.warn("API DELETE /api/v1/sensors/by-sensor-id/{}/permanent - HARD DELETE", sensorId);

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        DeleteSensorResponse response = sensorUpdateService.hardDeleteBySensorId(
                sensorId, request, userId);

        return ResponseEntity.ok(ApiResponse.<DeleteSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Khôi phục sensor đã bị soft delete
     * @param id UUID của sensor
     * @return Response xác nhận đã khôi phục
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<DeleteSensorResponse>> restoreSensor(
            @PathVariable(name = "id") UUID id) {

        log.info("API POST /api/v1/sensors/{}/restore - Restore sensor", id);

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        DeleteSensorResponse response = sensorUpdateService.restoreSensor(id, userId);

        return ResponseEntity.ok(ApiResponse.<DeleteSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     *
     * POST /api/v1/sensors/by-sensor-id/{sensorId}/restore
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/by-sensor-id/{sensorId}/restore")
    public ResponseEntity<ApiResponse<DeleteSensorResponse>> restoreBySensorId(
            @PathVariable(name = "sensorId") String sensorId) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        DeleteSensorResponse response = sensorUpdateService.restoreSensorBySensorId(
                sensorId, userId);

        return ResponseEntity.ok(ApiResponse.<DeleteSensorResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * PATCH /api/v1/sensors/{id}/status
     *
     * @param id      UUID của sensor
     * @param request Request chứa trạng thái mới và lý do
     * @return Response sau khi chuyển trạng thái
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ChangeStatusResponse>> changeStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody ChangeStatusRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();

        ChangeStatusResponse response = sensorUpdateService.changeStatus(id, request, userId);

        return ResponseEntity.ok(ApiResponse.<ChangeStatusResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * PATCH /api/v1/sensors/by-sensor-id/{sensorId}/status
     *
     * @param sensorId Mã sensor (VD: SENS-HAN-01)
     * @param request  Request chứa trạng thái mới và lý do
     * @return Response sau khi chuyển trạng thái
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/by-sensor-id/{sensorId}/status")
    public ResponseEntity<ApiResponse<ChangeStatusResponse>> changeStatusBySensorId(
            @PathVariable(name = "sensorId") String sensorId,
            @Valid @RequestBody ChangeStatusRequest request) {

        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
        log.info("API PATCH /api/v1/sensors/by-sensor-id/{}/status - Chuyển status sang '{}' bởi user: {}",
                sensorId, request.getNewStatus(), userId);

        ChangeStatusResponse response = sensorUpdateService.changeStatusBySensorId(sensorId, request, userId);

        log.info("API PATCH /api/v1/sensors/by-sensor-id/{}/status - Chuyển trạng thái thành công: {} → {}",
                sensorId, response.getPreviousStatus(), response.getCurrentStatus());

        return ResponseEntity.ok(ApiResponse.<ChangeStatusResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
