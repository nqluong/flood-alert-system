package org.project.floodalert.floodcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.common.exception.AppException;
import org.project.floodalert.floodcore.config.OrsProperties;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;
import org.project.floodalert.floodcore.dto.ors.OrsRequest;
import org.project.floodalert.floodcore.dto.request.SafeRouteRequest;
import org.project.floodalert.floodcore.dto.response.SafeRouteResponse;
import org.project.floodalert.floodcore.enums.VehicleType;
import org.project.floodalert.floodcore.exception.CoreErrorCode;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.SafeRoutingService;
import org.project.floodalert.floodcore.util.GeoUtils;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafeRoutingServiceImpl implements SafeRoutingService {

    private static final double BBOX_BUFFER = 0.03;  // Buffer 3km
    private static final double POLYGON_OFFSET = 0.002;  // Offset 200m
    private static final double MOTORBIKE_WATER_LEVEL_THRESHOLD = 15.0;  // Xe máy né ngập > 15cm
    private static final double CAR_WATER_LEVEL_THRESHOLD = 30.0;  // Ô tô né ngập > 30cm

    private static final String F_WATER_LEVEL = "waterLevel";
    private static final String F_LAT = "lat";
    private static final String F_LON = "lon";
    private static final String F_EVENT_ID = "eventId";

    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;
    private final OrsProperties orsProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public SafeRouteResponse findSafeRoute(SafeRouteRequest request) {
        log.info("[SafeRouting] Bắt đầu tìm đường an toàn từ ({},{}) đến ({},{}), phương tiện: {}",
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                request.getVehicleType());

        // Tính Bounding Box
        double[] bbox = GeoUtils.calculateBoundingBox(
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                BBOX_BUFFER
        );

        // Truy vấn Redis Geo để lấy danh sách ID điểm ngập trong bbox
        List<String> floodIds = queryFloodIdsInBoundingBox(bbox);

        if (floodIds.isEmpty()) {
            log.info("[SafeRouting] Không có điểm ngập trong vùng, gọi ORS trực tiếp");
            return callOrsWithoutAvoidance(request);
        }

        log.info("[SafeRouting] Tìm thấy {} điểm ngập trong Bounding Box", floodIds.size());

        // Truy vấn Redis Hash để lấy chi tiết (waterLevel) - Batch operation
        List<FloodDetail> floodDetails = batchGetFloodDetails(floodIds);

        // Lọc điểm ngập theo ngưỡng waterLevel
        double waterLevelThreshold = getWaterLevelThreshold(request.getVehicleType());
        List<FloodDetail> dangerousFloods = filterDangerousFloods(floodDetails, waterLevelThreshold);

        if (dangerousFloods.isEmpty()) {
            log.info("[SafeRouting] Không có điểm ngập nguy hiểm, gọi ORS trực tiếp");
            return callOrsWithoutAvoidance(request);
        }

        log.info("[SafeRouting] Có {} điểm ngập nguy hiểm cần tránh", dangerousFloods.size());

        // Tạo Polygon hình vuông bao quanh các điểm ngập
        List<List<List<List<Double>>>> avoidPolygons = createAvoidPolygons(dangerousFloods);

        // Gọi OpenRouteService với avoid_polygons
        String geoJson = callOrsWithAvoidance(request, avoidPolygons);

        return SafeRouteResponse.builder()
                .geoJson(geoJson)
                .avoidedFloodsCount(dangerousFloods.size())
                .message("Đã tìm thấy đường đi an toàn, tránh " + dangerousFloods.size() + " điểm ngập")
                .build();
    }

    /**
     * Truy vấn Redis Geo để lấy danh sách ID điểm ngập trong Bounding Box.
     */
    private List<String> queryFloodIdsInBoundingBox(double[] bbox) {
        try {
            String geoKey = redisKeyProperties.getKeys().getFlood().getGeoIndex();

            // bbox: [minLon, minLat, maxLon, maxLat]
            double minLon = bbox[0];
            double minLat = bbox[1];
            double maxLon = bbox[2];
            double maxLat = bbox[3];

            // Tính tâm và kích thước box
            double centerLon = (minLon + maxLon) / 2;
            double centerLat = (minLat + maxLat) / 2;
            double width = maxLon - minLon;
            double height = maxLat - minLat;

            double widthKm = width * 111;
            double heightKm = height * 111;

            log.debug("[SafeRouting] GEOSEARCH center=({},{}), width={}km, height={}km",
                    centerLat, centerLon, widthKm, heightKm);

            // Tính bán kính đường chéo của bbox
            double radiusKm = Math.sqrt(widthKm * widthKm + heightKm * heightKm) / 2;

            var geoResults = cacheService.geoRadius(
                    geoKey,
                    new Circle(
                            new Point(centerLon, centerLat),
                            new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS)
                    )
            );

            if (geoResults == null || geoResults.getContent().isEmpty()) {
                return Collections.emptyList();
            }

            List<String> floodIds = geoResults.getContent().stream()
                    .map(result -> String.valueOf(result.getContent().getName()))
                    .collect(Collectors.toList());

            log.debug("[SafeRouting] Tìm thấy {} điểm ngập trong bán kính, cần lọc lại theo bbox", floodIds.size());

            return floodIds;

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi truy vấn Redis Geo: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Truy vấn Redis Hash để lấy chi tiết các điểm ngập.
     * Tất cả HGETALL commands được gửi trong 1 network roundtrip.
     */
    private List<FloodDetail> batchGetFloodDetails(List<String> floodIds) {
        if (floodIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<String> detailKeys = floodIds.stream()
                    .map(eventId -> redisKeyProperties.getKeys().getFlood().getDetailKey(eventId))
                    .collect(Collectors.toList());

            log.debug("[SafeRouting] Batch query {} flood details bằng Redis Pipeline", detailKeys.size());

            List<Object> pipelineResults = cacheService.executePipeline(connection -> {
                for (String detailKey : detailKeys) {
                    connection.hashCommands().hGetAll(detailKey.getBytes());
                }
                return null;
            });

            List<FloodDetail> details = new ArrayList<>();
            for (int i = 0; i < pipelineResults.size(); i++) {
                try {
                    Object result = pipelineResults.get(i);
                    if (result == null) {
                        log.warn("[SafeRouting] Không tìm thấy detail cho eventId={}", floodIds.get(i));
                        continue;
                    }
                    Map<Object, Object> rawMap = convertBytesToStringMap(result);
                    
                    if (rawMap.isEmpty()) {
                        log.warn("[SafeRouting] Detail rỗng cho eventId={}", floodIds.get(i));
                        continue;
                    }

                    FloodDetail detail = mapToFloodDetail(rawMap);
                    if (detail != null && detail.getWaterLevel() != null) {
                        details.add(detail);
                    }

                } catch (Exception e) {
                    log.error("[SafeRouting] Lỗi khi parse detail cho eventId={}: {}", 
                            floodIds.get(i), e.getMessage());
                }
            }

            log.info("[SafeRouting] Batch query thành công: {} flood details từ {} keys (1 network roundtrip)",
                    details.size(), floodIds.size());
            return details;

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi batch query Redis: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> convertBytesToStringMap(Object result) {
        if (result instanceof Map) {
            Map<byte[], byte[]> byteMap = (Map<byte[], byte[]>) result;
            Map<Object, Object> stringMap = new HashMap<>();
            
            for (Map.Entry<byte[], byte[]> entry : byteMap.entrySet()) {
                String key = new String(entry.getKey());
                String value = entry.getValue() != null ? new String(entry.getValue()) : null;
                stringMap.put(key, value);
            }
            
            return stringMap;
        }
        return Collections.emptyMap();
    }

    private FloodDetail mapToFloodDetail(Map<Object, Object> rawMap) {
        try {
            String eventId = getString(rawMap, F_EVENT_ID);
            Double lat = parseDouble(rawMap, F_LAT);
            Double lon = parseDouble(rawMap, F_LON);
            Double waterLevel = parseDouble(rawMap, F_WATER_LEVEL);

            if (eventId == null || lat == null || lon == null || waterLevel == null) {
                log.warn("[SafeRouting] FloodDetail thiếu thông tin bắt buộc: eventId={}, lat={}, lon={}, waterLevel={}",
                        eventId, lat, lon, waterLevel);
                return null;
            }

            return FloodDetail.builder()
                    .eventId(eventId)
                    .lat(lat)
                    .lon(lon)
                    .waterLevel(waterLevel)
                    .build();

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi map FloodDetail: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lọc các điểm ngập nguy hiểm dựa trên ngưỡng waterLevel.
     * Xe máy né điểm ngập > 15cm, ô tô né điểm ngập > 30cm.
     */
    private List<FloodDetail> filterDangerousFloods(List<FloodDetail> floods, double threshold) {
        return floods.stream()
                .filter(flood -> flood.getWaterLevel() != null && flood.getWaterLevel() > threshold)
                .collect(Collectors.toList());
    }

    /**
     * Lấy ngưỡng waterLevel dựa trên loại phương tiện.
     */
    private double getWaterLevelThreshold(VehicleType vehicleType) {
        return vehicleType == VehicleType.MOTORBIKE
                ? MOTORBIKE_WATER_LEVEL_THRESHOLD
                : CAR_WATER_LEVEL_THRESHOLD;
    }

    /**
     * Tạo danh sách Polygon hình vuông bao quanh các điểm ngập.
     */
    private List<List<List<List<Double>>>> createAvoidPolygons(List<FloodDetail> floods) {
        List<List<List<List<Double>>>> multiPolygon = new ArrayList<>();

        for (FloodDetail flood : floods) {
            // Tạo hình vuông bao quanh điểm ngập
            List<List<Double>> squarePolygon = GeoUtils.createSquarePolygon(
                    flood.getLat(),
                    flood.getLon(),
                    POLYGON_OFFSET
            );

            // Wrap vào một layer nữa để đúng chuẩn MultiPolygon
            List<List<List<Double>>> polygon = new ArrayList<>();
            polygon.add(squarePolygon);

            multiPolygon.add(polygon);
        }

        log.debug("[SafeRouting] Đã tạo {} avoid polygons", multiPolygon.size());
        return multiPolygon;
    }

    /**
     * Gọi OpenRouteService API với avoid_polygons.
     */
    private String callOrsWithAvoidance(SafeRouteRequest request, List<List<List<List<Double>>>> avoidPolygons) {
        try {

            OrsRequest.OrsOptions.OrsOptionsBuilder optionsBuilder = OrsRequest.OrsOptions.builder()
                    .avoidPolygons(OrsRequest.AvoidPolygons.builder()
                            .coordinates(avoidPolygons)
                            .build());

            if (VehicleType.MOTORBIKE.equals(request.getVehicleType())) {
                optionsBuilder.avoidFeatures(List.of("highways", "tollways"));
            }
            // Tạo request body
            OrsRequest orsRequest = OrsRequest.builder()
                    .coordinates(List.of(
                            List.of(request.getStartLon(), request.getStartLat()),
                            List.of(request.getEndLon(), request.getEndLat())
                    ))
                    .options(optionsBuilder.build())
                    .build();

            // Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", orsProperties.getApiKey());

            HttpEntity<OrsRequest> entity = new HttpEntity<>(orsRequest, headers);

            log.debug("[SafeRouting] Gọi ORS API: {}", orsProperties.getApiUrl());

            // Gọi API
            ResponseEntity<String> response = restTemplate.exchange(
                    orsProperties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("[SafeRouting] ORS API trả về thành công");
                return response.getBody();
            } else {
                log.error("[SafeRouting] ORS API trả về status code: {}", response.getStatusCode());
                throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);
            }

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("[SafeRouting] ORS API rate limit (429): {}", e.getMessage());
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);

        } catch (ResourceAccessException e) {
            log.error("[SafeRouting] ORS API timeout: {}", e.getMessage());
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi gọi ORS API: {}", e.getMessage(), e);
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Gọi ORS API không có avoid_polygons (khi không có điểm ngập nguy hiểm).
     */
    private SafeRouteResponse callOrsWithoutAvoidance(SafeRouteRequest request) {
        try {

            OrsRequest orsRequest = OrsRequest.builder()
                    .coordinates(List.of(
                            List.of(request.getStartLon(), request.getStartLat()),
                            List.of(request.getEndLon(), request.getEndLat())
                    ))
                    .build();

            if (VehicleType.MOTORBIKE.equals(request.getVehicleType())) {

                OrsRequest.OrsOptions options = OrsRequest.OrsOptions.builder()
                        .avoidFeatures(List.of("highways", "tollways"))
                        .build();

                orsRequest.setOptions(options);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", orsProperties.getApiKey());

            HttpEntity<OrsRequest> entity = new HttpEntity<>(orsRequest, headers);
            try {
                String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orsRequest);
                log.info("[SafeRouting - Debug] JSON Payload gửi lên ORS:\n{}", jsonPayload);
            } catch (Exception e) {
                log.warn("[SafeRouting - Debug] Không thể in JSON Payload: {}", e.getMessage());
            }
            ResponseEntity<String> response = restTemplate.exchange(
                    orsProperties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return SafeRouteResponse.builder()
                        .geoJson(response.getBody())
                        .avoidedFloodsCount(0)
                        .message("Không có điểm ngập nguy hiểm trên đường đi")
                        .build();
            } else {
                throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);
            }

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi gọi ORS API: {}", e.getMessage(), e);
            throw new AppException(CoreErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private String getString(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Double parseDouble(Map<Object, Object> map, String key) {
        String val = getString(map, key);
        if (val == null || val.isBlank()) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            log.warn("[SafeRouting] Không thể parse Double, field='{}', value='{}'", key, val);
            return null;
        }
    }
}
