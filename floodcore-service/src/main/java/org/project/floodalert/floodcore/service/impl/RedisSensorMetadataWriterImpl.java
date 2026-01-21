package org.project.floodalert.floodcore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.model.Sensor;
import org.project.floodalert.floodcore.service.CacheService;
import org.project.floodalert.floodcore.service.SensorMetadataWriter;
import org.project.floodalert.floodcore.service.mapper.SensorMetadataMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSensorMetadataWriterImpl implements SensorMetadataWriter {
    private final CacheService cacheService;
    private final SensorMetadataMapper metadataMapper;

    private static final String SENSOR_INFO_KEY_PREFIX = "sensor:info:";

    /**
     * Ghi metadata của nhiều sensors vào Redis sử dụng Pipeline
     */
    @Override
    public void batchWriteSensorMetadata(List<Sensor> sensors) {
        Map<String, Map<String, String>> batchData = new HashMap<>();

        sensors.forEach(sensor -> {
            Map<String, String> metadata = metadataMapper.toMetadataMap(sensor);
            batchData.put(sensor.getSENSOR_ID(), metadata);
        });

        cacheService.batchHSet(SENSOR_INFO_KEY_PREFIX, batchData);

        log.debug("[METADATA] Đã ghi metadata cho {} sensors", sensors.size());
    }
}
