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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Component
public class ActiveLocationScanner {

    private static final String ACTIVE_LOCATIONS_KEY = "user:active_locations";
    private static final String USER_HEARTBEAT_PREFIX = "user:heartbeat:";

    private final RedisTemplate<String, String> geoRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GeoOperations<String, String> geoOps;

    public ActiveLocationScanner(
            @Qualifier("geoRedisTemplate") RedisTemplate<String, String> geoRedisTemplate,
            RedisTemplate<String, Object> redisTemplate) {
        this.geoRedisTemplate = geoRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.geoOps = geoRedisTemplate.opsForGeo();
    }

    public Map<UUID, NotificationContext> scan(FloodEventDTO event) {
        try {
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
                    ACTIVE_LOCATIONS_KEY,
                    new Circle(new Point(event.getLon(), event.getLat()),
                            new Distance(event.getRadiusMeters() / 1000.0, Metrics.KILOMETERS)),
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                            .includeDistance()
                            .includeCoordinates()
                            .sortAscending()
            );

            if (results == null || results.getContent().isEmpty()) {
                return Collections.emptyMap();
            }

            // Parse GEO result thành Map<userId, Entry>, dùng LinkedHashMap giữ thứ tự
            Map<UUID, Entry> userEntries = results.getContent().stream()
                    .map(this::tryParseGeoResult)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            Entry::userId,
                            e -> e,
                            (a, b) -> a,
                            LinkedHashMap::new));

            if (userEntries.isEmpty()) return Collections.emptyMap();

            // Multi-GET heartbeat (1 roundtrip cho N keys)
            List<UUID> orderedUserIds = List.copyOf(userEntries.keySet());
            List<String> heartbeatKeys = orderedUserIds.stream()
                    .map(id -> USER_HEARTBEAT_PREFIX + id)
                    .collect(Collectors.toList());
            List<Object> heartbeats = redisTemplate.opsForValue().multiGet(heartbeatKeys);

            if (heartbeats == null) return Collections.emptyMap();

            // Ghép heartbeat với userDistances, lọc user có heartbeat hợp lệ
            return IntStream.range(0, orderedUserIds.size())
                    .filter(i -> heartbeats.get(i) != null)
                    .boxed()
                    .collect(Collectors.toMap(
                            orderedUserIds::get,
                            i -> {
                                UUID userId = orderedUserIds.get(i);
                                Entry entry = userEntries.get(userId);
                                return NotificationContext.builder()
                                        .userId(userId)
                                        .isNearActive(true)
                                        .activeDistance(entry.distance())
                                        .activeLat(entry.lat())
                                        .activeLon(entry.lon())
                                        .build();
                            }));

        } catch (Exception e) {
            log.error("[Active] Lỗi khi quét active locations cho event {}", event.getEventId(), e);
            return Collections.emptyMap();
        }
    }

    private Entry tryParseGeoResult(GeoResult<RedisGeoCommands.GeoLocation<String>> result) {
        try {
            UUID userId = UUID.fromString(result.getContent().getName());
            double distanceMeters = result.getDistance().getValue() * 1000.0;
            Point point = result.getContent().getPoint();
            Double lon = point != null ? point.getX() : null;
            Double lat = point != null ? point.getY() : null;
            return new Entry(userId, distanceMeters, lat, lon);
        } catch (IllegalArgumentException e) {
            log.warn("[Active] Invalid userId format: {}", result.getContent().getName());
            return null;
        }
    }

    private record Entry(UUID userId, Double distance, Double lat, Double lon) {}
}
