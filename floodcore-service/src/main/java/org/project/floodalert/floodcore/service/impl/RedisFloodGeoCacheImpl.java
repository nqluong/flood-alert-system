package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.config.RedisKeyProperties;
import org.project.floodalert.floodcore.dto.event.FloodLifecycleEvent;
import org.project.floodalert.floodcore.dto.response.ActiveFloodResponse;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.FloodGeoCache;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisFloodGeoCacheImpl implements FloodGeoCache {

    private static final String F_EVENT_ID = "eventId";
    private static final String F_LAT = "lat";
    private static final String F_LON = "lon";
    private static final String F_SEVERITY = "severityLevel";
    private static final String F_WATER_LEVEL = "waterLevel";
    private static final String F_LOCATION = "location";
    private static final String F_UPDATED_AT = "updatedAt";

    private final CacheService cacheService;
    private final RedisKeyProperties redisKeyProperties;

    @Override
    public void cacheActiveFlood(FloodLifecycleEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("[FloodGeoCache] Bỏ qua event null hoặc thiếu eventId");
            return;
        }
        if (event.getLat() == null || event.getLon() == null) {
            log.warn("[FloodGeoCache] Bỏ qua event thiếu toạ độ, eventId={}", event.getEventId());
            return;
        }

        try {
            String geoKey = redisKeyProperties.getKeys().getFlood().getGeoIndex();
            String detailKey = redisKeyProperties.getKeys().getFlood().getDetailKey(event.getEventId());
            long ttlSecs = redisKeyProperties.getTtl().getFloodDetail();

            cacheService.geoAdd(geoKey, new Point(event.getLon(), event.getLat()), event.getEventId());

            cacheService.hSetAll(detailKey, buildHashFields(event));
            cacheService.expire(detailKey, ttlSecs, TimeUnit.SECONDS);

            log.debug("[FloodGeoCache] Đã cache eventId={}, lat={}, lon={}",
                    event.getEventId(), event.getLat(), event.getLon());

        } catch (Exception e) {
            log.error("[FloodGeoCache] Lỗi khi cache eventId={}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    @Override
    public void removeFloodCache(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("[FloodGeoCache] removeFloodCache: eventId rỗng, bỏ qua");
            return;
        }
        try {
            String geoKey = redisKeyProperties.getKeys().getFlood().getGeoIndex();
            String detailKey = redisKeyProperties.getKeys().getFlood().getDetailKey(eventId);

            cacheService.geoRemove(geoKey, eventId);
            cacheService.delete(detailKey);

            log.debug("[FloodGeoCache] Đã xóa cache eventId={}", eventId);

        } catch (Exception e) {
            log.error("[FloodGeoCache] Lỗi khi xóa cache eventId={}: {}", eventId, e.getMessage(), e);
        }
    }

    @Override
    public List<ActiveFloodResponse> findFloodsInRadius(double lat, double lon, double radiusKm) {
        try {
            String geoKey = redisKeyProperties.getKeys().getFlood().getGeoIndex();

            Circle searchArea = new Circle(
                    new Point(lon, lat),
                    new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS)
            );

            GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults =
                    cacheService.geoRadius(geoKey, searchArea);

            if (geoResults == null || geoResults.getContent().isEmpty()) {
                log.debug("[FloodGeoCache] Không tìm thấy điểm ngập trong {}km tại ({},{})",
                        radiusKm, lat, lon);
                return Collections.emptyList();
            }

            List<ActiveFloodResponse> result = new ArrayList<>();

            for (GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult
                    : geoResults.getContent()) {

                String eventId = String.valueOf(geoResult.getContent().getName());
                String detailKey = redisKeyProperties.getKeys().getFlood().getDetailKey(eventId);

                Map<Object, Object> rawMap = cacheService.hGetAll(detailKey);

                if (rawMap == null || rawMap.isEmpty()) {
                    log.warn("[FloodGeoCache] Không có detail cho eventId={}, bỏ qua", eventId);
                    continue;
                }

                result.add(mapToResponse(rawMap));
            }

            log.debug("[FloodGeoCache] Tìm thấy {} điểm ngập trong bán kính {}km", result.size(), radiusKm);
            return result;

        } catch (Exception e) {
            log.error("[FloodGeoCache] Lỗi khi tìm điểm ngập tại ({},{}): {}", lat, lon, e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    @Override
    public void clearGeoIndex() {
        String geoKey = redisKeyProperties.getKeys().getFlood().getGeoIndex();
        cacheService.delete(geoKey);
        log.debug("[FloodGeoCache] Đã xóa toàn bộ GEO index '{}'", geoKey);
    }


    private Map<String, Object> buildHashFields(FloodLifecycleEvent event) {
        Map<String, Object> fields = new HashMap<>(8);
        fields.put(F_EVENT_ID, event.getEventId());
        fields.put(F_LAT, String.valueOf(event.getLat()));
        fields.put(F_LON, String.valueOf(event.getLon()));
        fields.put(F_SEVERITY, event.getSeverityLevel() != null ? event.getSeverityLevel() : "");
        fields.put(F_WATER_LEVEL, event.getWaterLevel() != null ? String.valueOf(event.getWaterLevel()) : "");
        fields.put(F_LOCATION, event.getLocation() != null ? event.getLocation() : "");
        fields.put(F_UPDATED_AT, LocalDateTime.now().toString());
        return fields;
    }

    private ActiveFloodResponse mapToResponse(Map<Object, Object> rawMap) {
        return ActiveFloodResponse.builder()
                .eventId(getString(rawMap, F_EVENT_ID))
                .lat(parseDouble(rawMap, F_LAT))
                .lon(parseDouble(rawMap, F_LON))
                .severityLevel(getString(rawMap, F_SEVERITY))
                .waterLevel(parseDouble(rawMap, F_WATER_LEVEL))
                .location(getString(rawMap, F_LOCATION))
                .updatedAt(getString(rawMap, F_UPDATED_AT))
                .build();
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
            log.warn("[FloodGeoCache] Không thể parse Double, field='{}', value='{}'", key, val);
            return null;
        }
    }
}

