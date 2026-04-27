package org.project.floodalert.floodcore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.ActiveFloodResponse;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.service.FloodGeoCache;
import org.project.floodalert.floodcore.service.UserReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/core/floods")
@RequiredArgsConstructor
public class MapApiController {

    private final FloodGeoCache floodGeoCache;
    private final UserReportService userReportService;

    /**
     * Lấy danh sách điểm ngập trong bán kính quanh vị trí người dùng.
     *
     * <p>Ví dụ: {@code GET /api/v1/core/floods/active/nearby?lat=10.776&lon=106.700&radius=5}
     *
     * @param lat    vĩ độ tâm tìm kiếm (độ thập phân)
     * @param lon    kinh độ tâm tìm kiếm (độ thập phân)
     * @param radius bán kính tìm kiếm tính bằng km (mặc định 10 km)
     * @return danh sách điểm ngập active trong bán kính
     */
    @GetMapping("/active/nearby")
    public ResponseEntity<ApiResponse<List<ActiveFloodResponse>>> getNearbyActiveFloods(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lon") double lon,
            @RequestParam(name = "radius",defaultValue = "10") double radius
    ) {
        List<ActiveFloodResponse> floods = floodGeoCache.findFloodsInRadius(lat, lon, radius);

        return ResponseEntity.ok(ApiResponse.success(floods));
    }

//    @PostMapping("/user-report")
//    public ResponseEntity<ApiResponse<UserReportResponse>> getUserReport(@RequestBody UserReportRequest userReportRequest) {
//        UUID userId = SecurityContextUtils.getCurrentUserIdAsUUID();
//        UserReportResponse userReportResponse = userReportService.submitUserReport(userReportRequest, userId);
//        return ResponseEntity.ok(ApiResponse.success(userReportResponse));
//    }
}

