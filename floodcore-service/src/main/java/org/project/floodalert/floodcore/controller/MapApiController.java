package org.project.floodalert.floodcore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.dto.response.ApiResponse;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.dto.request.UserReportRequest;
import org.project.floodalert.floodcore.dto.response.ActiveFloodResponse;
import org.project.floodalert.floodcore.dto.response.SafeRouteResponse;
import org.project.floodalert.floodcore.dto.response.UserReportResponse;
import org.project.floodalert.floodcore.enums.VehicleType;
import org.project.floodalert.floodcore.service.FloodGeoCache;
import org.project.floodalert.floodcore.service.SafeRoutingService;
import org.project.floodalert.floodcore.service.UserReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/core")
@RequiredArgsConstructor
public class MapApiController {

    private final FloodGeoCache floodGeoCache;
    private final UserReportService userReportService;
    private final SafeRoutingService safeRoutingService;

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
    @GetMapping("/floods/active/nearby")
    public ResponseEntity<ApiResponse<List<ActiveFloodResponse>>> getNearbyActiveFloods(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lon") double lon,
            @RequestParam(name = "radius",defaultValue = "10") double radius
    ) {
        List<ActiveFloodResponse> floods = floodGeoCache.findFloodsInRadius(lat, lon, radius);

        return ResponseEntity.ok(ApiResponse.success(floods));
    }

    /**
     * Tìm đường đi an toàn từ điểm A đến điểm B, tránh các điểm ngập nặng.
     *
     * <p>Ví dụ: {@code GET /api/v1/core/routes/safe-path?startLat=10.776&startLon=106.700&endLat=10.780&endLon=106.710&vehicleType=MOTORBIKE}
     *
     * @param startLat    vĩ độ điểm xuất phát
     * @param startLon    kinh độ điểm xuất phát
     * @param endLat      vĩ độ điểm đến
     * @param endLon      kinh độ điểm đến
     * @param vehicleType loại phương tiện (MOTORBIKE hoặc CAR)
     * @return GeoJSON route từ OpenRouteService
     */
    @GetMapping("/routes/safe-path")
    public ResponseEntity<ApiResponse<SafeRouteResponse>> getSafePath(
            @RequestParam(name = "startLat") double startLat,
            @RequestParam(name = "startLon") double startLon,
            @RequestParam(name = "endLat") double endLat,
            @RequestParam(name = "endLon") double endLon,
            @RequestParam(name = "vehicleType") VehicleType vehicleType
    ) {
        log.info("[MapApiController] Nhận request tìm đường an toàn: ({},{}) -> ({},{}), vehicle={}",
                startLat, startLon, endLat, endLon, vehicleType);
        Instant startTime = Instant.now();
        SafeRouteRequest request = SafeRouteRequest.builder()
                .startLat(startLat)
                .startLon(startLon)
                .endLat(endLat)
                .endLon(endLon)
                .vehicleType(vehicleType)
                .build();

        SafeRouteResponse response = safeRoutingService.findSafeRoute(request);
        log.info("[MapApiController] Trả về route an toàn sau {}ms",
                Instant.now().toEpochMilli() - startTime.toEpochMilli());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

