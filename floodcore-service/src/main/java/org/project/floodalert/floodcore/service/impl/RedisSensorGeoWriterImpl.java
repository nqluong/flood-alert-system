package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.SensorGeoWriter;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSensorGeoWriterImpl implements SensorGeoWriter {
    private final CacheService cacheService;

    private static final String GEO_ALL_SENSORS_KEY = "maps:all_sensors";

    /**
     * Ghi tọa độ địa lý của nhiều sensors vào Redis Geo sử dụng Pipeline
     */
    @Override
    public void batchWriteSensorGeoLocation(List<Sensor> sensors) {
        cacheService.executePipeline((RedisCallback<Object>) connection -> {

            sensors.forEach(sensor -> {
                try {
                    // Kiểm tra tọa độ hợp lệ
                    if (sensor.getLAT() == null || sensor.getLON() == null) {
                        log.warn("[GEO] Sensor {} không có tọa độ", sensor.getSENSOR_ID());
                        return;
                    }

                    Point point = new Point(
                            sensor.getLON().doubleValue(),
                            sensor.getLAT().doubleValue()
                    );

                    // Tạo GeoLocation
                    RedisGeoCommands.GeoLocation<byte[]> geoLocation =
                            new RedisGeoCommands.GeoLocation<>(
                                    sensor.getSENSOR_ID().getBytes(),
                                    point
                            );

                    // Thêm vào Redis Geo
                    connection.geoCommands().geoAdd(
                            GEO_ALL_SENSORS_KEY.getBytes(),
                            geoLocation
                    );

                } catch (Exception e) {
                    log.error("[GEO] Lỗi khi ghi tọa độ cho sensor {}: {}",
                            sensor.getSENSOR_ID(), e.getMessage());
                }
            });

            return null;
        });

        log.debug("[GEO] Đã ghi tọa độ cho {} sensors", sensors.size());
    }
}
