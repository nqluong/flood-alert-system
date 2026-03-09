//package org.project.floodalert.floodprocessor.service.aggregator.impl;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.project.floodalert.floodprocessor.model.FloodEvent;
//import org.project.floodalert.floodprocessor.service.aggregator.FloodGeoCacheService;
//import org.springframework.data.geo.Point;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Cấu trúc Redis:
// * <pre>
// *   GEOADD api:floods:geo  <lon> <lat> <eventId>       ← tọa độ để query gần đây (GEORADIUS)
// *   HSET   api:floods:details <eventId> <JSON chi tiết> ← thông tin đầy đủ của sự kiện
// * </pre>
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class FloodGeoCacheServiceImpl implements FloodGeoCacheService {
//
//    /** Key của Redis GEO set lưu tọa độ các điểm ngập đang active */
//    private static final String GEO_KEY = "api:floods:geo";
//
//    /** Key của Redis Hash lưu chi tiết JSON của từng flood event */
//    private static final String DETAILS_HASH_KEY = "api:floods:details";
//
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final ObjectMapper objectMapper;
//
//    /**
//     * {@inheritDoc}
//     * Thực hiện GEOADD và HSET để thêm/cập nhật sự kiện ngập vào cache.
//     */
//    @Override
//    public void syncActiveFloodEvent(FloodEvent floodEvent) {
//        String eventId = floodEvent.getEventId();
//
//        if (eventId == null || floodEvent.getLon() == null || floodEvent.getLat() == null) {
//            log.warn("Không thể sync Redis: eventId, lat hoặc lon bị null cho sự kiện [{}]", eventId);
//            return;
//        }
//
//        try {
//            // Lưu tọa độ vào GEO Set (GEOADD api:floods:geo <lon> <lat> <eventId>)
//            addToGeoSet(floodEvent);
//
//            // Lưu chi tiết vào Hash (HSET api:floods:details <eventId> <JSON>)
//            addToDetailHash(floodEvent);
//
//            log.info("Đồng bộ Redis thành công: sự kiện [{}] tại [{}, {}]",
//                    eventId, floodEvent.getLat(), floodEvent.getLon());
//
//        } catch (Exception e) {
//            // Lỗi cache không nên dừng luồng chính → log cảnh báo
//            log.warn("Đồng bộ Redis thất bại cho sự kiện [{}]: {}", eventId, e.getMessage());
//        }
//    }
//
//    /**
//     * {@inheritDoc}
//     * Thực hiện ZREM và HDEL để xóa sự kiện ngập đã RESOLVED khỏi cache.
//     */
//    @Override
//    public void removeResolvedFloodEvent(String eventId) {
//        if (eventId == null) {
//            log.warn("Không thể xóa Redis: eventId bị null");
//            return;
//        }
//
//        try {
//            // Xóa khỏi GEO Set (ZREM api:floods:geo <eventId>)
//            Long removedFromGeo = redisTemplate.opsForZSet().remove(GEO_KEY, eventId);
//
//            // Xóa khỏi Details Hash (HDEL api:floods:details <eventId>)
//            Long removedFromHash = redisTemplate.opsForHash().delete(DETAILS_HASH_KEY, eventId);
//
//            log.info("Đã xóa sự kiện RESOLVED [{}] khỏi Redis (geo={}, hash={})",
//                    eventId, removedFromGeo, removedFromHash);
//
//        } catch (Exception e) {
//            log.warn("Xóa Redis thất bại cho sự kiện [{}]: {}", eventId, e.getMessage());
//        }
//    }
//
//    /**
//     * Thêm tọa độ của flood event vào Redis GEO Set.
//     * Lệnh tương đương: GEOADD api:floods:geo <lon> <lat> <eventId>
//     */
//    private void addToGeoSet(FloodEvent floodEvent) {
//        // Spring Data Redis GeoOperations nhận Point(lon, lat)
//        Point coordinate = new Point(
//                floodEvent.getLon().doubleValue(),
//                floodEvent.getLat().doubleValue()
//        );
//        redisTemplate.opsForGeo().add(GEO_KEY, coordinate, floodEvent.getEventId());
//        log.debug("GEOADD [{}] tại ({}, {})", floodEvent.getEventId(),
//                floodEvent.getLon(), floodEvent.getLat());
//    }
//
//    /**
//     * Lưu thông tin chi tiết của flood event vào Redis Hash dưới dạng JSON.
//     * Lệnh tương đương: HSET api:floods:details <eventId> <JSON>
//     */
//    private void addToDetailHash(FloodEvent floodEvent) throws JsonProcessingException {
//        Map<String, Object> detail = buildDetailMap(floodEvent);
//        String jsonDetail = objectMapper.writeValueAsString(detail);
//
//        redisTemplate.opsForHash().put(DETAILS_HASH_KEY, floodEvent.getEventId(), jsonDetail);
//        log.debug("HSET chi tiết sự kiện [{}]", floodEvent.getEventId());
//    }
//
//    /**
//     * Xây dựng map chứa thông tin cần thiết để Mobile App hiển thị flood marker.
//     */
//    private Map<String, Object> buildDetailMap(FloodEvent floodEvent) {
//        Map<String, Object> detail = new HashMap<>();
//        detail.put("eventId", floodEvent.getEventId());
//        detail.put("waterLevel", floodEvent.getWaterLevel());
//        detail.put("severity", floodEvent.getSeverityLevel());
//        detail.put("location", floodEvent.getLocationDescription());
//        detail.put("lat", floodEvent.getLat());
//        detail.put("lon", floodEvent.getLon());
//        detail.put("status", floodEvent.getStatus());
//        detail.put("expiresAt", floodEvent.getExpiresAt() != null
//                ? floodEvent.getExpiresAt().toString() : null);
//        return detail;
//    }
//}
