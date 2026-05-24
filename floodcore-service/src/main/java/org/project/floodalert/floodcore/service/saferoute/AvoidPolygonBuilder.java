package org.project.floodalert.floodcore.service.saferoute;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.project.floodalert.floodcore.dto.internal.FloodDetail;
import org.project.floodalert.floodcore.util.GeoUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class AvoidPolygonBuilder {

    public static final double POLYGON_OFFSET = 0.001;

    /**
     * Tạo danh sách polygon hình vuông bao quanh các điểm ngập nguy hiểm.
     */
    public static List<List<List<Double>>> build(List<FloodDetail> floods) {
        List<List<List<Double>>> polygons = floods.stream()
                .map(flood -> GeoUtils.createSquarePolygon(flood.getLat(), flood.getLon(), POLYGON_OFFSET))
                .collect(Collectors.toList());

        log.debug("[SafeRouting] Đã tạo {} avoid polygons", polygons.size());
        return polygons;
    }
}
