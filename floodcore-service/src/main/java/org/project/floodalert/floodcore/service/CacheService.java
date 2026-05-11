package org.project.floodalert.floodcore.service;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface CacheService {
    // String operations
    void set(String key, Object value);
    void set(String key, Object value, long timeout, TimeUnit unit);
    Object get(String key);
    Boolean delete(String key);
    Boolean exists(String key);
    Boolean expire(String key, long timeout, TimeUnit unit);

    // Hash operations
    void hSet(String key, String field, Object value);
    void hSetAll(String key, Map<String, Object> map);
    Object hGet(String key, String field);
    Map<Object, Object> hGetAll(String key);
    Long hDelete(String key, Object... fields);
    Boolean hExists(String key, String field);

    // Geo operations
    Long geoAdd(String key, Point point, String member);
    GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadius(String key, Circle circle);
    GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadiusByMember(String key, String member, Distance distance);
    List<Point> geoPos(String key, String... members);
    Distance geoDist(String key, String member1, String member2);
    Long geoRemove(String key, String... members);

    // Set operations
    Long sAdd(String key, Object... values);
    Set<Object> sMembers(String key);
    Boolean sIsMember(String key, Object value);
    Long sRemove(String key, Object... values);

    // Pipeline operations
    List<Object> executePipeline(RedisCallback<Object> callback);
    void batchHSet(String keyPrefix, Map<String, Map<String, String>> data);
    void batchDelete(List<String> keys);
    
    /**
     * Batch insert nhiều Hash keys với full key names (không cần prefix)
     * Tối ưu cho việc insert hàng loạt sensors vào Redis
     * 
     * @param data Map<fullKeyName, Map<field, value>>
     */
    void batchHSetFullKeys(Map<String, Map<String, String>> data);

    // Utility
    Set<String> keys(String pattern);
}
