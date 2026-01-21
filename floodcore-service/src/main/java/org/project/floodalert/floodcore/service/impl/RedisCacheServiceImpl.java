package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.service.CacheService;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Lưu giá trị String vào Redis
     */
    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Lưu giá trị String với TTL
     */
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * Lấy giá trị String từ Redis
     */
    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Xóa key khỏi Redis
     */
    @Override
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * Kiểm tra key có tồn tại không
     */
    @Override
    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * Set TTL cho key
     */
    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    // ==================== HASH OPERATIONS ====================

    /**
     * Lưu một field vào Hash
     */
    @Override
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Lưu nhiều field vào Hash cùng lúc
     */
    @Override
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * Lấy giá trị của một field trong Hash
     */
    @Override
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * Lấy toàn bộ Hash
     */
    @Override
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Xóa field khỏi Hash
     */
    @Override
    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * Kiểm tra field có tồn tại trong Hash không
     */
    @Override
    public Boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    // ==================== GEO OPERATIONS ====================

    /**
     * Thêm một điểm vào Redis Geo
     */
    @Override
    public Long geoAdd(String key, Point point, String member) {
        return redisTemplate.opsForGeo().add(key, point, member);
    }

    /**
     * Tìm kiếm các điểm trong bán kính
     */
    @Override
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadius(
            String key, Circle circle) {
        return redisTemplate.opsForGeo().radius(key, circle);
    }

    /**
     * Tìm kiếm các điểm gần một tọa độ cụ thể
     */
    @Override
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadiusByMember(
            String key, String member, Distance distance) {
        return redisTemplate.opsForGeo().radius(key, member, distance);
    }

    /**
     * Lấy tọa độ của một member
     */
    @Override
    public List<Point> geoPos(String key, String... members) {
        return redisTemplate.opsForGeo().position(key, members);
    }

    /**
     * Tính khoảng cách giữa 2 điểm
     */
    @Override
    public Distance geoDist(String key, String member1, String member2) {
        return redisTemplate.opsForGeo().distance(key, member1, member2);
    }

    /**
     * Xóa member khỏi Geo
     */
    @Override
    public Long geoRemove(String key, String... members) {
        return redisTemplate.opsForGeo().remove(key, members);
    }

    /**
     * Thêm phần tử vào Set
     */
    @Override
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * Lấy toàn bộ phần tử trong Set
     */
    @Override
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Kiểm tra phần tử có trong Set không
     */
    @Override
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * Xóa phần tử khỏi Set
     */
    @Override
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    // ==================== PIPELINE OPERATIONS ====================

    /**
     * Thực thi pipeline cho batch operations
     * Giảm network round-trips khi cần thực hiện nhiều lệnh
     */
    @Override
    public List<Object> executePipeline(RedisCallback<Object> callback) {
        return redisTemplate.executePipelined(callback);
    }

    /**
     * Batch set nhiều Hash fields sử dụng pipeline
     */
    @Override
    public void batchHSet(String keyPrefix, Map<String, Map<String, String>> data) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            data.forEach((suffix, fields) -> {
                String key = keyPrefix + suffix;
                byte[] keyBytes = key.getBytes();

                fields.forEach((field, value) -> {
                    connection.hashCommands().hSet(
                            keyBytes,
                            field.getBytes(),
                            value.getBytes()
                    );
                });
            });
            return null;
        });
    }

    /**
     * Batch delete nhiều keys sử dụng pipeline
     */
    @Override
    public void batchDelete(List<String> keys) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> {
                connection.keyCommands().del(key.getBytes());
            });
            return null;
        });
    }

    /**
     * Lấy tất cả keys theo pattern
     * CHÚ Ý: Không nên dùng trong production với dataset lớn
     */
    @Override
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

}
