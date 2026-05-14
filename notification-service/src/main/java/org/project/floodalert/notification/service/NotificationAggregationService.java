package org.project.floodalert.notification.service;

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
public class NotificationAggregationService {
    
    private final RedisTemplate<String, String> geoRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private GeoOperations<String, String> geoOps;
    
    public NotificationAggregationService(
            @Qualifier("geoRedisTemplate") RedisTemplate<String, String> geoRedisTemplate,
            RedisTemplate<String, Object> redisTemplate) {
        this.geoRedisTemplate = geoRedisTemplate;
        this.redisTemplate = redisTemplate;
    }
    
    private static final String ACTIVE_LOCATIONS_KEY = "user:active_locations";
    private static final String GEO_STATIC_LOCATIONS_KEY = "user_locations:static";
    private static final String ADDRESS_DETAILS_PREFIX = "address_details:";
    private static final String USER_HEARTBEAT_PREFIX = "user:heartbeat:";
    private static final long USER_HEARTBEAT_TTL_SECONDS = 300L; // 5 phút
    
    private GeoOperations<String, String> getGeoOps() {
        if (geoOps == null) {
            geoOps = geoRedisTemplate.opsForGeo();
        }
        return geoOps;
    }

    public Map<UUID, NotificationContext> aggregateNotificationContexts(FloodEventDTO event) {
        log.debug("Bắt đầu quét Redis Geo cho event: eventId={}, location=({}, {}), radius={}m",
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
            
            log.debug("Quét Redis hoàn tất: {} active users, {} static locations",
                    activeResults.size(), staticResults.size());
            
            Map<UUID, NotificationContext> mergedContexts = mergeContexts(activeResults, staticResults);
            
            log.debug("Aggregation hoàn tất: Tổng {} unique users bị ảnh hưởng", mergedContexts.size());
            
            return mergedContexts;
            
        } catch (Exception e) {
            log.error("Lỗi khi aggregate notification contexts cho event: {}", event.getEventId(), e);
            return Collections.emptyMap();
        }
    }
    

    private Map<UUID, NotificationContext> scanActiveLocations(FloodEventDTO event) {
        try {
            log.debug("Quét ACTIVE locations...");
            
            Point center = new Point(event.getLon(), event.getLat());
            Distance radius = new Distance(event.getRadiusMeters() / 1000.0, Metrics.KILOMETERS);
            Circle area = new Circle(center, radius);
            
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = getGeoOps().radius(
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
            
            log.debug("Tìm thấy {} active locations, đang kiểm tra heartbeat...", results.getContent().size());
            
            Map<UUID, Double> userDistances = new HashMap<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
                try {
                    String memberStr = result.getContent().getName();
                    UUID userId = UUID.fromString(memberStr);
                    Double distanceMeters = result.getDistance().getValue() * 1000;
                    userDistances.put(userId, distanceMeters);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid userId format: {}", result.getContent().getName());
                }
            }
            
            if (userDistances.isEmpty()) {
                return Collections.emptyMap();
            }
            
            List<String> heartbeatKeys = userDistances.keySet().stream()
                    .map(userId -> USER_HEARTBEAT_PREFIX + userId)
                    .collect(Collectors.toList());
            
            List<Object> heartbeatResults = redisTemplate.opsForValue().multiGet(heartbeatKeys);
            
            Map<UUID, NotificationContext> contexts = new ConcurrentHashMap<>();
            int index = 0;
            for (UUID userId : userDistances.keySet()) {
                Object heartbeat = heartbeatResults != null && index < heartbeatResults.size() 
                        ? heartbeatResults.get(index) 
                        : null;
                
                if (heartbeat != null) {
                    Double distanceMeters = userDistances.get(userId);
                    
                    NotificationContext context = NotificationContext.builder()
                            .userId(userId)
                            .isNearActive(true)
                            .activeDistance(distanceMeters)
                            .build();
                    
                    contexts.put(userId, context);
                } else {
                    log.debug("User {} không có heartbeat hợp lệ, bỏ qua", userId);
                }
                
                index++;
            }
            
            return contexts;
            
        } catch (Exception e) {
            log.error("Lỗi khi quét active locations", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Quét static locations (địa điểm cố định của user)
     * GEO quét addressId
     * HASH lấy chi tiết (userId, zoneName)
     */
    private Map<UUID, NotificationContext> scanStaticLocations(FloodEventDTO event) {
        try {
            log.debug("Quét STATIC locations...");
            
            // Quét GEO để lấy danh sách addressId
            Point center = new Point(event.getLon(), event.getLat());
            Distance radius = new Distance(event.getRadiusMeters() / 1000.0, Metrics.KILOMETERS);
            Circle area = new Circle(center, radius);
            
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = getGeoOps().radius(
                    GEO_STATIC_LOCATIONS_KEY,
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
            
            log.debug("Tìm thấy {} địa chỉ trong vùng ngập", results.getContent().size());
            
            // Lấy chi tiết từ HASH và gộp theo userId
            Map<UUID, NotificationContext> contexts = new ConcurrentHashMap<>();
            
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
                try {
                    String addressId = result.getContent().getName();
                    
                    if (addressId.startsWith("\"") && addressId.endsWith("\"")) {
                        addressId = addressId.substring(1, addressId.length() - 1);
                    }
                    
                    Double distanceMeters = result.getDistance().getValue() * 1000;
                    
                    // Lấy chi tiết từ HASH
                    String hashKey = ADDRESS_DETAILS_PREFIX + addressId;
                    Map<Object, Object> details = redisTemplate.opsForHash().entries(hashKey);
                    
                    if (details.isEmpty()) {
                        log.warn("Không tìm thấy chi tiết cho addressId={}", addressId);
                        continue;
                    }
                    
                    String userIdStr = (String) details.get("userId");
                    String zoneName = (String) details.get("zoneName");
                    
                    if (userIdStr == null || zoneName == null) {
                        log.warn("Thiếu thông tin trong HASH: addressId={}", addressId);
                        continue;
                    }
                    
                    UUID userId = UUID.fromString(userIdStr);
                    
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
                    log.warn("Lỗi parse addressId hoặc userId: {}", result.getContent().getName(), e);
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
        
        staticResults.forEach((userId, staticContext) -> merged.merge(userId, staticContext, (existing, incoming) -> {
            existing.getAffectedZones().addAll(incoming.getAffectedZones());
            existing.setStaticDistance(incoming.getStaticDistance());
            return existing;
        }));
        
        long bothAffected = merged.values().stream().filter(NotificationContext::isBothAffected).count();
        long onlyActive = merged.values().stream()
                .filter(ctx -> ctx.isNearActive() && ctx.getAffectedZones().isEmpty()).count();
        long onlyStatic = merged.values().stream()
                .filter(ctx -> !ctx.isNearActive() && !ctx.getAffectedZones().isEmpty()).count();
        
        log.debug("Phân loại users: {} dính cả 2, {} chỉ active, {} chỉ static",
                bothAffected, onlyActive, onlyStatic);
        
        return merged;
    }

    public String generateNotificationBody(NotificationContext context, FloodEventDTO event) {
        String severityVi = translateSeverityToVietnamese(event.getSeverityLevel());
        
        if (context.isBothAffected()) {
            String zones = String.join(", ", context.getAffectedZones());
            return String.format(
                    "Khu vực bạn đang di chuyển VÀ gần %s đang có ngập lụt (mức độ: %s). Hãy chú ý an toàn!",
                    zones, severityVi
            );
        } else if (context.isNearActive()) {
            return String.format(
                    "Có điểm ngập lụt cách vị trí hiện tại của bạn khoảng %.0fm (mức độ: %s). Chú ý hướng di chuyển!",
                    context.getActiveDistance(), severityVi
            );
        } else {
            String zones = String.join(", ", context.getAffectedZones());
            return String.format(
                    "Cảnh báo: Khu vực quanh %s của bạn vừa xuất hiện điểm ngập (mức độ: %s).",
                    zones, severityVi
            );
        }
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
            default -> severityLevel;
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
