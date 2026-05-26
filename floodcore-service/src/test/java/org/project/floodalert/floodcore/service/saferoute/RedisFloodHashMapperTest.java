package org.project.floodalert.floodcore.service.saferoute;

import org.junit.jupiter.api.Test;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RedisFloodHashMapperTest {

    @Test
    void toStringMap_rawResultNull_returnsEmptyMap() {
        Map<String, String> result =
                RedisFloodHashMapper.toStringMap(null);

        assertThat(result).isEmpty();
    }

    @Test
    void toStringMap_rawResultNotMap_returnsEmptyMap() {
        Map<String, String> result =
                RedisFloodHashMapper.toStringMap("invalid");

        assertThat(result).isEmpty();
    }

    @Test
    void toStringMap_rawMapEmpty_returnsEmptyMap() {
        Map<String, String> result =
                RedisFloodHashMapper.toStringMap(Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void toStringMap_stringValues_mapsCorrectly() {
        Map<Object, Object> rawMap = new HashMap<>();
        rawMap.put("eventId", "flood-001");
        rawMap.put("lat", "10.5");
        rawMap.put("lon", "106.7");

        Map<String, String> result =
                RedisFloodHashMapper.toStringMap(rawMap);

        assertThat(result)
                .containsEntry("eventId", "flood-001")
                .containsEntry("lat", "10.5")
                .containsEntry("lon", "106.7");
    }

    @Test
    void toStringMap_byteArrayValues_mapsCorrectly() {
        Map<Object, Object> rawMap = new HashMap<>();
        rawMap.put("eventId".getBytes(), "flood-001".getBytes());
        rawMap.put("lat".getBytes(), "10.5".getBytes());

        Map<String, String> result =
                RedisFloodHashMapper.toStringMap(rawMap);

        assertThat(result)
                .containsEntry("eventId", "flood-001")
                .containsEntry("lat", "10.5");
    }

    @Test
    void toFloodDetail_rawMapNull_returnsEmpty() {
        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(null);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_rawMapEmpty_returnsEmpty() {
        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_validMap_returnsFloodDetail() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "10.5",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isPresent();

        FloodDetail detail = result.get();

        assertThat(detail.getEventId()).isEqualTo("flood-001");
        assertThat(detail.getLat()).isEqualTo(10.5);
        assertThat(detail.getLon()).isEqualTo(106.7);
        assertThat(detail.getWaterLevel()).isEqualTo(1.2);
    }

    @Test
    void toFloodDetail_missingEventId_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "lat", "10.5",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_missingLat_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_missingLon_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "10.5",
                "waterLevel", "1.2"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_missingWaterLevel_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "10.5",
                "lon", "106.7"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_invalidLat_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "invalid",
                "lon", "106.7",
                "waterLevel", "1.2"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_invalidLon_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "10.5",
                "lon", "invalid",
                "waterLevel", "1.2"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_invalidWaterLevel_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", "flood-001",
                "lat", "10.5",
                "lon", "106.7",
                "waterLevel", "invalid"
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }

    @Test
    void toFloodDetail_blankValues_returnsEmpty() {
        Map<String, String> rawMap = Map.of(
                "eventId", " ",
                "lat", " ",
                "lon", " ",
                "waterLevel", " "
        );

        Optional<FloodDetail> result =
                RedisFloodHashMapper.toFloodDetail(rawMap);

        assertThat(result).isEmpty();
    }
}
