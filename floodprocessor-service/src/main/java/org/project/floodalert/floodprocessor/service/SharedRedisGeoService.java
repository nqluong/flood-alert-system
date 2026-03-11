package org.project.floodalert.floodprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedRedisGeoService {

    static final String ACTIVE_FLOODS_GEO_KEY = "active_floods_geo";

    private final RedisTemplate<String, Object> redisTemplate;

    public Optional<String> findNearbyActiveFlood(double lat, double lon, double radiusKm) {
        log.debug("[GEO-SERVICE] GEOSEARCH active_floods_geo FROMLONLAT lon={} lat={} BYRADIUS {}km",
                lon, lat, radiusKm);

        try {
            Circle searchArea = new Circle(
                    new Point(lon, lat),
                    new Distance(radiusKm, Metrics.KILOMETERS)
            );

            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .sortAscending()
                    .limit(1);

            GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                    redisTemplate.opsForGeo().radius(ACTIVE_FLOODS_GEO_KEY, searchArea, args);

            if (results == null || results.getContent().isEmpty()) {
                log.debug("[GEO-SERVICE] Không tìm thấy điểm ngập nào trong bán kính {}km tại ({}, {})",
                        radiusKm, lat, lon);
                return Optional.empty();
            }

            String eventId = String.valueOf(
                    results.getContent().get(0).getContent().getName()
            );

            log.debug("[GEO-SERVICE] Tìm thấy điểm ngập gần nhất: eventId={}, khoảng cách={}",
                    eventId, results.getContent().get(0).getDistance());

            return Optional.of(eventId);

        } catch (Exception e) {
            log.error("[GEO-SERVICE] Lỗi khi truy vấn Redis Geo tại ({}, {}): {}", lat, lon, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
