package org.project.floodalert.floodcore.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @Mock
    private GeoOperations<String, Object> geoOps;

    @Mock
    private SetOperations<String, Object> setOps;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisHashCommands redisHashCommands;

    @Mock
    private RedisKeyCommands redisKeyCommands;

    @InjectMocks
    private RedisCacheServiceImpl redisCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        when(redisConnection.hashCommands()).thenReturn(redisHashCommands);
        when(redisConnection.keyCommands()).thenReturn(redisKeyCommands);

        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            callback.doInRedis(redisConnection);
            return List.of();
        });
    }

    @Test
    void set_withoutTtl() {
        redisCacheService.set("key", "value");
    }

    @Test
    void set_withTtl() {
        redisCacheService.set("key", "value", 60L, TimeUnit.SECONDS);
    }

    @Test
    void get_returnsValue() {
        when(valueOps.get("key")).thenReturn("value");
        redisCacheService.get("key");
    }

    @Test
    void delete_returnsBoolean() {
        when(redisTemplate.delete("key")).thenReturn(true);
        redisCacheService.delete("key");
    }

    @Test
    void exists_returnsBoolean() {
        when(redisTemplate.hasKey("key")).thenReturn(true);
        redisCacheService.exists("key");
    }

    @Test
    void expire_returnsBoolean() {
        when(redisTemplate.expire("key", 60L, TimeUnit.SECONDS)).thenReturn(true);
        redisCacheService.expire("key", 60L, TimeUnit.SECONDS);
    }

    @Test
    void hSet_setsField() {
        redisCacheService.hSet("key", "field", "value");
    }

    @Test
    void hSetAll_setsMultipleFields() {
        redisCacheService.hSetAll("key", Map.of("f1", "v1", "f2", "v2"));
    }

    @Test
    void hGet_returnsFieldValue() {
        when(hashOps.get("key", "field")).thenReturn("value");
        redisCacheService.hGet("key", "field");
    }

    @Test
    void hGetAll_returnsAllFields() {
        redisCacheService.hGetAll("key");
    }

    @Test
    void hDelete_deletesFields() {
        redisCacheService.hDelete("key", "f1", "f2");
    }

    @Test
    void hExists_returnsBoolean() {
        redisCacheService.hExists("key", "field");
    }

    @Test
    void geoAdd_addsPoint() {
        redisCacheService.geoAdd("geo-key", new Point(106.5, 10.5), "member");
    }

    @Test
    void geoRadius_returnsResults() {
        redisCacheService.geoRadius("geo-key", new Circle(new Point(106.5, 10.5),
                new Distance(5, RedisGeoCommands.DistanceUnit.KILOMETERS)));
    }

    @Test
    void geoRadiusByMember_returnsResults() {
        redisCacheService.geoRadiusByMember("geo-key", "member",
                new Distance(5, RedisGeoCommands.DistanceUnit.KILOMETERS));
    }

    @Test
    void geoPos_returnsPositions() {
        redisCacheService.geoPos("geo-key", "m1", "m2");
    }

    @Test
    void geoDist_returnsDistance() {
        redisCacheService.geoDist("geo-key", "m1", "m2");
    }

    @Test
    void geoRemove_removesMembers() {
        redisCacheService.geoRemove("geo-key", "m1", "m2");
    }

    // --- Set operations ---

    @Test
    void sAdd_addsMembers() {
        redisCacheService.sAdd("set-key", "v1", "v2");
    }

    @Test
    void sMembers_returnsMembers() {
        redisCacheService.sMembers("set-key");
    }

    @Test
    void sIsMember_returnsBoolean() {
        redisCacheService.sIsMember("set-key", "value");
    }

    @Test
    void sRemove_removesMembers() {
        redisCacheService.sRemove("set-key", "v1");
    }

    @Test
    void executePipeline_executesCallback() {
        redisCacheService.executePipeline(connection -> {
            connection.hashCommands();
            return null;
        });
    }

    @Test
    void batchHSet_executesLambdaWithData() {
        Map<String, Map<String, String>> data = Map.of(
                "S001", Map.of("status", "ACTIVE", "name", "Sensor 1"),
                "S002", Map.of("status", "DISABLED")
        );
        redisCacheService.batchHSet("sensor:info:", data);
    }

    @Test
    void batchHSet_emptyData_executesLambda() {
        redisCacheService.batchHSet("sensor:info:", Map.of());
    }

    @Test
    void batchHSetFullKeys_executesLambdaWithData() {
        Map<String, Map<String, String>> data = Map.of(
                "sensor:info:S001", Map.of("status", "ACTIVE", "name", "Sensor 1")
        );
        redisCacheService.batchHSetFullKeys(data);
    }

    @Test
    void batchHSetFullKeys_emptyData_executesLambda() {
        redisCacheService.batchHSetFullKeys(Map.of());
    }

    @Test
    void batchDelete_executesLambdaWithKeys() {
        redisCacheService.batchDelete(List.of("key1", "key2", "key3"));
    }

    @Test
    void batchDelete_emptyList_executesLambda() {
        redisCacheService.batchDelete(List.of());
    }


    @Test
    void keys_returnsMatchingKeys() {
        redisCacheService.keys("sensor:*");
    }
}
