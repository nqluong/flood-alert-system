package org.project.floodalert.floodcore.service.saferoute;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class RedisFloodHashMapper {

    public static final String F_WATER_LEVEL = "waterLevel";
    public static final String F_LAT = "lat";
    public static final String F_LON = "lon";
    public static final String F_EVENT_ID = "eventId";

    /**
     * Convert raw Redis HGETALL result (có thể là byte[] hoặc String) thành Map<String,String>.
     */
    public static Map<String, String> toStringMap(Object rawResult) {
        if (!(rawResult instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }

        return rawMap.entrySet().stream()
                .map(entry -> Map.entry(
                        decode(entry.getKey()),
                        decode(entry.getValue())))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a));
    }

    /**
     * Map raw Redis Hash sang FloodDetail
     */
    public static Optional<FloodDetail> toFloodDetail(Map<String, String> rawMap) {
        if (rawMap == null || rawMap.isEmpty()) {
            return Optional.empty();
        }

        String eventId = rawMap.get(F_EVENT_ID);
        Double lat = parseDouble(rawMap.get(F_LAT), F_LAT);
        Double lon = parseDouble(rawMap.get(F_LON), F_LON);
        Double waterLevel = parseDouble(rawMap.get(F_WATER_LEVEL), F_WATER_LEVEL);

        if (eventId == null || lat == null || lon == null || waterLevel == null) {
            log.warn("[SafeRouting] FloodDetail thiếu thông tin bắt buộc: eventId={}, lat={}, lon={}, waterLevel={}",
                    eventId, lat, lon, waterLevel);
            return Optional.empty();
        }

        return Optional.of(FloodDetail.builder()
                .eventId(eventId)
                .lat(lat)
                .lon(lon)
                .waterLevel(waterLevel)
                .build());
    }

    private static String decode(Object value) {
        if (value == null) return null;
        if (value instanceof byte[] bytes) return new String(bytes);
        if (value instanceof String s) return s;
        return Objects.toString(value, null);
    }

    private static Double parseDouble(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("[SafeRouting] Không thể parse Double, field='{}', value='{}'", fieldName, value);
            return null;
        }
    }
}
