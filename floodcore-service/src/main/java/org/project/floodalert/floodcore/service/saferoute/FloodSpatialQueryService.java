package org.project.floodalert.floodcore.service.saferoute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;
import org.project.floodalert.floodcore.service.CacheService;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FloodSpatialQueryService {

    private static final double DEG_TO_KM = 111.0;

    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;

    /**
     * Truy vấn Redis Geo (GEOSEARCH) lấy danh sách flood ID trong bounding box.
     */
    public List<String> findFloodIdsInBoundingBox(double[] bbox) {
        try {
            double centerLon = (bbox[0] + bbox[2]) / 2.0;
            double centerLat = (bbox[1] + bbox[3]) / 2.0;
            double widthKm = (bbox[2] - bbox[0]) * DEG_TO_KM;
            double heightKm = (bbox[3] - bbox[1]) * DEG_TO_KM;
            double radiusKm = Math.hypot(widthKm, heightKm) / 2.0;

            log.debug("[SafeRouting] GEOSEARCH center=({},{}), radius={}km", centerLat, centerLon, radiusKm);

            GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = cacheService.geoRadius(
                    redisKeyProperties.getKeys().getFlood().getGeoIndex(),
                    new Circle(new Point(centerLon, centerLat),
                            new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS))
            );

            if (geoResults == null || geoResults.getContent().isEmpty()) {
                return Collections.emptyList();
            }

            return geoResults.getContent().stream()
                    .map(result -> String.valueOf(result.getContent().getName()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi truy vấn Redis Geo: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Batch lấy chi tiết các flood (HGETALL) trong 1 network roundtrip qua pipeline.
     * Dùng IntStream để ghép pipeline result với eventId, lọc qua mapper, không có for-loop thủ công.
     */
    public List<FloodDetail> batchGetFloodDetails(List<String> floodIds) {
        if (floodIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            byte[][] detailKeys = floodIds.stream()
                    .map(id -> redisKeyProperties.getKeys().getFlood().getDetailKey(id))
                    .map(key -> key.getBytes(StandardCharsets.UTF_8))
                    .toArray(byte[][]::new);

            log.debug("[SafeRouting] Batch query {} flood details bằng Redis Pipeline", detailKeys.length);

            List<Object> pipelineResults = cacheService.executePipeline(connection -> {
                for (byte[] key : detailKeys) {
                    connection.hashCommands().hGetAll(key);
                }
                return null;
            });

            if (pipelineResults == null || pipelineResults.isEmpty()) {
                return Collections.emptyList();
            }

            int size = Math.min(pipelineResults.size(), floodIds.size());
            List<FloodDetail> details = IntStream.range(0, size)
                    .mapToObj(i -> parseSingleResult(pipelineResults.get(i), floodIds.get(i)))
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toList());

            log.info("[SafeRouting] Pipeline lấy {} flood details từ {} keys", details.size(), floodIds.size());
            return details;

        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi khi batch query Redis: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private java.util.Optional<FloodDetail> parseSingleResult(Object result, String eventId) {
        if (result == null) {
            log.warn("[SafeRouting] Không tìm thấy detail cho eventId={}", eventId);
            return java.util.Optional.empty();
        }
        try {
            return RedisFloodHashMapper.toFloodDetail(RedisFloodHashMapper.toStringMap(result));
        } catch (Exception e) {
            log.error("[SafeRouting] Lỗi parse detail cho eventId={}: {}", eventId, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
