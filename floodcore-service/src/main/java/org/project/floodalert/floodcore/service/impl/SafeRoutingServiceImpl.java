package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;
import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.dto.response.SafeRouteResponse;
import org.project.floodalert.floodcore.service.SafeRoutingService;
import org.project.floodalert.floodcore.service.saferoute.AvoidPolygonBuilder;
import org.project.floodalert.floodcore.service.saferoute.FloodSpatialQueryService;
import org.project.floodalert.floodcore.service.saferoute.OrsRouteClient;
import org.project.floodalert.floodcore.service.saferoute.WaterLevelThresholdResolver;
import org.project.floodalert.floodcore.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafeRoutingServiceImpl implements SafeRoutingService {

    private static final double BBOX_BUFFER = 0.03;

    private final FloodSpatialQueryService floodSpatialQueryService;
    private final OrsRouteClient orsRouteClient;

    @Override
    public SafeRouteResponse findSafeRoute(SafeRouteRequest request) {
        log.info("[SafeRouting] Bắt đầu tìm đường an toàn từ ({},{}) đến ({},{}), phương tiện: {}",
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                request.getVehicleType());

        List<FloodDetail> dangerousFloods = resolveDangerousFloods(request);

        if (dangerousFloods.isEmpty()) {
            return buildSafeResponse(orsRouteClient.call(request, null), 0,
                    "Không có điểm ngập nguy hiểm trên đường đi");
        }

        log.info("[SafeRouting] Có {} điểm ngập nguy hiểm cần tránh", dangerousFloods.size());
        List<List<List<Double>>> avoidPolygons = AvoidPolygonBuilder.build(dangerousFloods);
        String geoJson = orsRouteClient.call(request, avoidPolygons);

        return buildSafeResponse(geoJson, dangerousFloods.size(),
                "Đã tìm thấy đường đi an toàn, tránh " + dangerousFloods.size() + " điểm ngập");
    }

    private List<FloodDetail> resolveDangerousFloods(SafeRouteRequest request) {
        double[] bbox = GeoUtils.calculateBoundingBox(
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                BBOX_BUFFER);

        List<String> floodIds = floodSpatialQueryService.findFloodIdsInBoundingBox(bbox);
        if (floodIds.isEmpty()) {
            log.info("[SafeRouting] Không có điểm ngập trong vùng");
            return Collections.emptyList();
        }
        log.info("[SafeRouting] Tìm thấy {} điểm ngập trong Bounding Box", floodIds.size());

        List<FloodDetail> details = floodSpatialQueryService.batchGetFloodDetails(floodIds);
        double threshold = WaterLevelThresholdResolver.resolve(request.getVehicleType());

        return details.stream()
                .filter(flood -> flood.getWaterLevel() != null && flood.getWaterLevel() > threshold)
                .collect(Collectors.toList());
    }

    private SafeRouteResponse buildSafeResponse(String geoJson, int avoidedCount, String message) {
        return SafeRouteResponse.builder()
                .geoJson(geoJson)
                .avoidedFloodsCount(avoidedCount)
                .message(message)
                .build();
    }
}
