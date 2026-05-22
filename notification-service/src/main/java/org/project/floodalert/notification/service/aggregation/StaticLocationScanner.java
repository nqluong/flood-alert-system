package org.project.floodalert.notification.service.aggregation;

import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FloodEventDTO;
import org.project.floodalert.notification.model.NotificationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class StaticLocationScanner {

    private static final String GEO_STATIC_LOCATIONS_KEY = "user_locations:static";
    private static final String ADDRESS_DETAILS_PREFIX = "address_details:";
    private static final String F_USER_ID = "userId";
    private static final String F_ZONE_NAME = "zoneName";

    private final RedisTemplate<String, String> geoRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GeoOperations<String, String> geoOps;

    public StaticLocationScanner(
            @Qualifier("geoRedisTemplate") RedisTemplate<String, String> geoRedisTemplate,
            RedisTemplate<String, Object> redisTemplate) {
        this.geoRedisTemplate = geoRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.geoOps = geoRedisTemplate.opsForGeo();
    }

    public Map<UUID, NotificationContext> scan(FloodEventDTO event) {
        try {
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
                    GEO_STATIC_LOCATIONS_KEY,
                    new Circle(new Point(event.getLon(), event.getLat()),
                            new Distance(event.getRadiusMeters() / 1000.0, Metrics.KILOMETERS)),
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                            .includeDistance()
                            .sortAscending()
            );

            if (results == null || results.getContent().isEmpty()) {
                return Collections.emptyMap();
            }

            // Trích addressId + distance từ GEO result
            List<AddressHit> hits = results.getContent().stream()
                    .map(this::toHit)
                    .collect(Collectors.toList());

            // /PIPELINE HGETALL cho tất cả addressId 1
            List<Map<Object, Object>> details = pipelineHGetAll(hits);

            // Gộp theo userId thành NotificationContext
            return mergeByUserId(hits, details);

        } catch (Exception e) {
            log.error("[Static] Lỗi khi quét static locations cho event {}", event.getEventId(), e);
            return Collections.emptyMap();
        }
    }

    private AddressHit toHit(GeoResult<RedisGeoCommands.GeoLocation<String>> result) {
        String addressId = result.getContent().getName();
        // Strip quote nếu có (tồn tại trong code cũ)
        if (addressId.length() >= 2
                && addressId.charAt(0) == '"'
                && addressId.charAt(addressId.length() - 1) == '"') {
            addressId = addressId.substring(1, addressId.length() - 1);
        }
        double distanceMeters = result.getDistance().getValue() * 1000.0;
        return new AddressHit(addressId, distanceMeters);
    }

    /**
     * Batch HGETALL qua Redis Pipeline. N keys → 1 network roundtrip.
     */
    @SuppressWarnings("unchecked")
    private List<Map<Object, Object>> pipelineHGetAll(List<AddressHit> hits) {
        byte[][] keys = hits.stream()
                .map(hit -> (ADDRESS_DETAILS_PREFIX + hit.addressId()).getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);

        List<Object> rawResults = redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (byte[] key : keys) {
                connection.hashCommands().hGetAll(key);
            }
            return null;
        });

        if (rawResults == null) return Collections.emptyList();

        return rawResults.stream()
                .map(r -> r instanceof Map ? (Map<Object, Object>) r : Collections.<Object, Object>emptyMap())
                .collect(Collectors.toList());
    }

    private Map<UUID, NotificationContext> mergeByUserId(List<AddressHit> hits, List<Map<Object, Object>> details) {
        Map<UUID, NotificationContext> contexts = new HashMap<>();
        int size = Math.min(hits.size(), details.size());

        IntStream.range(0, size).forEach(i -> {
            AddressHit hit = hits.get(i);
            Map<Object, Object> raw = details.get(i);
            if (raw.isEmpty()) {
                log.debug("[Static] Address {} không có HASH details", hit.addressId());
                return;
            }

            String userIdStr = asString(raw.get(F_USER_ID));
            String zoneName = asString(raw.get(F_ZONE_NAME));
            if (userIdStr == null || zoneName == null) {
                log.warn("[Static] Thiếu userId hoặc zoneName cho addressId={}", hit.addressId());
                return;
            }

            UUID userId;
            try {
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("[Static] Invalid userId in HASH: {}", userIdStr);
                return;
            }

            contexts.compute(userId, (key, existing) -> {
                if (existing == null) {
                    Map<String, Double> zoneDistances = new HashMap<>();
                    zoneDistances.put(zoneName, hit.distance());
                    List<String> zones = new ArrayList<>();
                    zones.add(zoneName);
                    return NotificationContext.builder()
                            .userId(userId)
                            .affectedZones(zones)
                            .zoneDistances(zoneDistances)
                            .staticDistance(hit.distance())
                            .build();
                }
                existing.getAffectedZones().add(zoneName);
                existing.getZoneDistances().merge(zoneName, hit.distance(), Math::min);
                if (existing.getStaticDistance() == null || hit.distance() < existing.getStaticDistance()) {
                    existing.setStaticDistance(hit.distance());
                }
                return existing;
            });
        });

        return contexts;
    }

    private static String asString(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        return value.toString();
    }

    private record AddressHit(String addressId, Double distance) {}
}
