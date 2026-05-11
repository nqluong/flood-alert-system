package org.project.floodalert.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.notification.dto.event.FloodEventDTO;
import org.project.floodalert.notification.model.NotificationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAggregationService {
    
    @Qualifier("geoRedisTemplate")
    private final RedisTemplate<String, String> geoRedisTemplate;
    
    private static final String ACTIVE_LOCATIONS_KEY = "user:active_locations";
    private static final String STATIC_LOCATIONS_KEY = "user:static_locations";
    private static final String COMPOUND_KEY_SEPARATOR = "::";

    public Map<UUID, NotificationContext> aggregateNotificationContexts(FloodEventDTO event) {
        log.info("Bắt đầu quét Redis Geo cho event: eventId={}, location=({}, {}), radius={}m",
                event.getEventId(), event.getLat(), event.getLon(), event.getRadiusMeters());
        
        CompletableFuture<Map<UUID, NotificationContext>> activeFuture =
                CompletableFuture.supplyAsync(() -> scanActiveLocations(event));
        
        CompletableFuture<Map<UUID, NotificationContext>> staticFuture = 
                CompletableFuture.supplyAsync(() -> scanStaticLocations(event));
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(activeFuture, staticFuture);
        
        try {
            allFutures.join();
            
            Map<UUID, NotificationContext> activeResults = activeFuture.get();
            Map<UUID, NotificationContext> staticResults = staticFuture.get();
            
            log.info("Quét Redis hoàn tất: {} active users, {} static locations",
                    activeResults.size(), staticResults.size());
            
            Map<UUID, NotificationContext> mergedContexts = mergeContexts(activeResults, staticResults);
            
            log.info("Aggregation hoàn tất: Tổng {} unique users bị ảnh hưởng", mergedContexts.size());
            
            return mergedContexts;
            
        } catch (Exception e) {
            log.error("Lỗi khi aggregate notification contexts cho event: {}", event.getEventId(), e);
            return Collections.emptyMap();
        }
    }
    

    private Map<UUID, NotificationContext> scanActiveLocations(FloodEventDTO event) {
        try {
            log.debug("Quét ACTIVE locations...");
            
            GeoOperations<String, String> geoOps = geoRedisTemplate.opsForGeo();
            Point center = new Point(event.getLon(), event.getLat());
            Distance radius = new Distance(event.getRadiusMeters() / 1000.0, Metrics.KILOMETERS);
            Circle area = new Circle(center, radius);
            
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
                    ACTIVE_LOCATIONS_KEY,
                    area,
                    RedisGeoCommands.GeoRadiusCommandArgs
                            .newGeoRadiusArgs()
                            .includeDistance()
                            .sortAscending()
            );
            
            if (results == null || results.getContent().isEmpty()) {
                log.debug("Không tìm thấy active locations nào");
                return Collections.emptyMap();
            }
            
            Map<UUID, NotificationContext> contexts = new ConcurrentHashMap<>();
            
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
                try {
                    String memberStr = result.getContent().getName();
                    UUID userId = UUID.fromString(memberStr);
                    Double distanceMeters = result.getDistance().getValue() * 1000;
                    
                    NotificationContext context = NotificationContext.builder()
                            .userId(userId)
                            .isNearActive(true)
                            .activeDistance(distanceMeters)
                            .build();
                    
                    contexts.put(userId, context);
                    
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid userId format in active locations: {}", 
                            result.getContent().getName());
                }
            }
            
            log.debug("Tìm thấy {} active users", contexts.size());
            return contexts;
            
        } catch (Exception e) {
            log.error("Lỗi khi quét active locations", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Quét static locations (địa điểm cố định của user)
     * Member format: {userId}::{addressId}::{zoneName}
     */
    private Map<UUID, NotificationContext> scanStaticLocations(FloodEventDTO event) {
        try {
            log.debug("Quét STATIC locations...");
            
            GeoOperations<String, String> geoOps = geoRedisTemplate.opsForGeo();
            Point center = new Point(event.getLon(), event.getLat());
            Distance radius = new Distance(event.getRadiusMeters() / 1000.0, Metrics.KILOMETERS);
            Circle area = new Circle(center, radius);
            
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOps.radius(
                    STATIC_LOCATIONS_KEY,
                    area,
                    RedisGeoCommands.GeoRadiusCommandArgs
                            .newGeoRadiusArgs()
                            .includeDistance()
                            .sortAscending()
            );
            
            if (results == null || results.getContent().isEmpty()) {
                log.debug("Không tìm thấy static locations nào");
                return Collections.emptyMap();
            }
            
            Map<UUID, NotificationContext> contexts = new ConcurrentHashMap<>();
            
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
                try {
                    String compoundKey = result.getContent().getName();
                    Double distanceMeters = result.getDistance().getValue() * 1000;
                    
                    // Parse compound key: {userId}::{addressId}::{zoneName}
                    String[] parts = compoundKey.split(COMPOUND_KEY_SEPARATOR);
                    
                    if (parts.length < 3) {
                        log.warn("Invalid compound key format (expected userId::addressId::zoneName): {}", compoundKey);
                        continue;
                    }
                    
                    UUID userId = UUID.fromString(parts[0]);
                    // parts[1] là addressId, bỏ qua vì không cần
                    String zoneName = parts[2];
                    
                    // Gộp vào context (có thể đã có từ active)
                    contexts.compute(userId, (key, existingContext) -> {
                        if (existingContext == null) {
                            return NotificationContext.builder()
                                    .userId(userId)
                                    .affectedZones(new ArrayList<>(List.of(zoneName)))
                                    .staticDistance(distanceMeters)
                                    .build();
                        } else {
                            existingContext.getAffectedZones().add(zoneName);
                            if (existingContext.getStaticDistance() == null || 
                                    distanceMeters < existingContext.getStaticDistance()) {
                                existingContext.setStaticDistance(distanceMeters);
                            }
                            return existingContext;
                        }
                    });
                    
                } catch (IllegalArgumentException e) {
                    log.warn("Lỗi parse compound key: {}", result.getContent().getName(), e);
                }
            }
            
            log.debug("Tìm thấy {} unique users từ static locations", contexts.size());
            return contexts;
            
        } catch (Exception e) {
            log.error("Lỗi khi quét static locations", e);
            return Collections.emptyMap();
        }
    }

    private Map<UUID, NotificationContext> mergeContexts(
            Map<UUID, NotificationContext> activeResults,
            Map<UUID, NotificationContext> staticResults) {
        
        Map<UUID, NotificationContext> merged = new ConcurrentHashMap<>(activeResults);
        
        staticResults.forEach((userId, staticContext) -> {
            merged.merge(userId, staticContext, (existing, incoming) -> {
                // Merge thông tin từ static vào existing (từ active)
                existing.getAffectedZones().addAll(incoming.getAffectedZones());
                existing.setStaticDistance(incoming.getStaticDistance());
                return existing;
            });
        });
        
        // Log chi tiết
        long bothAffected = merged.values().stream().filter(NotificationContext::isBothAffected).count();
        long onlyActive = merged.values().stream()
                .filter(ctx -> ctx.isNearActive() && ctx.getAffectedZones().isEmpty()).count();
        long onlyStatic = merged.values().stream()
                .filter(ctx -> !ctx.isNearActive() && !ctx.getAffectedZones().isEmpty()).count();
        
        log.info("Phân loại users: {} dính cả 2, {} chỉ active, {} chỉ static",
                bothAffected, onlyActive, onlyStatic);
        
        return merged;
    }
    
    /**
     * Sinh nội dung thông báo dựa trên context
     */
    public String generateNotificationBody(NotificationContext context, FloodEventDTO event) {
        String severityVi = translateSeverityToVietnamese(event.getSeverityLevel());
        
        if (context.isBothAffected()) {
            // Dính cả 2
            String zones = extractZoneNames(context.getAffectedZones());
            return String.format(
                    "Khu vực bạn đang di chuyển VÀ gần %s đang có ngập lụt (mức độ: %s). Hãy chú ý an toàn!",
                    zones, severityVi
            );
        } else if (context.isNearActive()) {
            // Chỉ dính active
            return String.format(
                    "Có điểm ngập lụt cách vị trí hiện tại của bạn khoảng %.0fm (mức độ: %s). Chú ý hướng di chuyển!",
                    context.getActiveDistance(), severityVi
            );
        } else {
            // Chỉ dính static
            String zones = extractZoneNames(context.getAffectedZones());
            return String.format(
                    "Cảnh báo: Khu vực quanh %s của bạn vừa xuất hiện điểm ngập (mức độ: %s).",
                    zones, severityVi
            );
        }
    }
    
    /**
     * Trích xuất tên địa chỉ từ compound keys (loại bỏ UUID)
     * Input: ["uuid1::Hồng Mai", "uuid2::Đống Đa"]
     * Output: "Hồng Mai, Đống Đa"
     */
    private String extractZoneNames(List<String> affectedZones) {
        return affectedZones.stream()
                .map(zone -> {
                    // Nếu có dấu ::, lấy phần sau
                    int separatorIndex = zone.indexOf(COMPOUND_KEY_SEPARATOR);
                    if (separatorIndex != -1 && separatorIndex < zone.length() - 2) {
                        return zone.substring(separatorIndex + 2);
                    }
                    // Nếu không có, trả về nguyên bản
                    return zone;
                })
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Chuyển đổi mức độ ngập từ tiếng Anh sang tiếng Việt
     */
    private String translateSeverityToVietnamese(String severityLevel) {
        if (severityLevel == null) {
            return "Không xác định";
        }
        
        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL" -> "Cực kỳ nguy hiểm";
            case "DANGER", "HIGH" -> "Nguy hiểm";
            case "WARNING", "MEDIUM" -> "Cảnh báo";
            case "LOW" -> "Thấp";
            default -> severityLevel; // Giữ nguyên nếu không match
        };
    }

    public String generateNotificationTitle(NotificationContext context) {
        if (context.isBothAffected()) {
            return "Cảnh báo ngập lụt khẩn cấp";
        } else if (context.isNearActive()) {
            return "Ngập lụt gần vị trí của bạn";
        } else {
            return "Cảnh báo ngập lụt khu vực";
        }
    }
}
