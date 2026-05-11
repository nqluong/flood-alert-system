package org.project.floodalert.floodcore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.floodcore.dto.request.VirtualSensorProvisionRequest;
import org.project.floodalert.floodcore.dto.response.VirtualSensorCleanupResponse;
import org.project.floodalert.floodcore.dto.response.VirtualSensorProvisionResponse;
import org.project.floodalert.floodcore.service.VirtualSensorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/sensors/virtual")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;

    /**
     * API Khởi tạo (Provisioning) Virtual Sensors
     * 
     * POST /api/v1/sensors/virtual/provision
     * 
     * Request Body:
     * {
     *   "target_count": 50
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "code": 201,
     *   "data": {
     *     "total_count": 50,
     *     "created_count": 20,
     *     "existing_count": 30,
     *     "sensors": [
     *       {
     *         "sensor_id": "VIRTUAL_0001",
     *         "api_key": "uuid-string",
     *         "lat": 21.0285,
     *         "lon": 105.8542,
     *         "name": "Virtual Sensor 1",
     *         "status": "ACTIVE"
     *       },
     *       ...
     *     ],
     *     "message": "Đã tạo thêm 20 virtual sensors. Tổng cộng: 50 sensors"
     *   }
     * }
     * 
     * @param request Request chứa target_count
     * @return Response chứa danh sách tất cả virtual sensors
     */
    @PostMapping("/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VirtualSensorProvisionResponse>> provisionVirtualSensors(
            @Valid @RequestBody VirtualSensorProvisionRequest request) {

        log.info("API POST /api/v1/sensors/virtual/provision - Target count: {}", 
                request.getTargetCount());

        VirtualSensorProvisionResponse response = virtualSensorService.provisionVirtualSensors(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<VirtualSensorProvisionResponse>builder()
                        .success(true)
                        .code(HttpStatus.CREATED.value())
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * API Dọn dẹp (Cleanup) toàn bộ Virtual Sensors
     * 
     * Logic:
     * 1. Tìm tất cả sensors có is_virtual = true
     * 2. Xóa khỏi Redis trước (để Ingestion Service không nhận data)
     * 3. Xóa khỏi PostgreSQL
     * 
     * DELETE /api/v1/sensors/virtual/clean
     * 
     * Response:
     * {
     *   "success": true,
     *   "code": 200,
     *   "data": {
     *     "deleted_count": 50,
     *     "message": "Đã xóa thành công 50 virtual sensors khỏi hệ thống"
     *   }
     * }
     * 
     * @return Response chứa số lượng đã xóa
     */
    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VirtualSensorCleanupResponse>> cleanupVirtualSensors() {

        log.warn("API DELETE /api/v1/sensors/virtual/clean - Bắt đầu cleanup virtual sensors");

        VirtualSensorCleanupResponse response = virtualSensorService.cleanupVirtualSensors();

        return ResponseEntity.ok(ApiResponse.<VirtualSensorCleanupResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
