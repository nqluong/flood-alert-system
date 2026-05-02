package org.project.floodalert.floodcore.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class GeoUtils {

    /**
     * Tính toán Bounding Box bao trùm 2 điểm A và B, cộng thêm buffer.
     *
     * <p>Công thức:
     * <ul>
     *   <li>minLon = min(lonA, lonB) - buffer</li>
     *   <li>maxLon = max(lonA, lonB) + buffer</li>
     *   <li>minLat = min(latA, latB) - buffer</li>
     *   <li>maxLat = max(latA, latB) + buffer</li>
     * </ul>
     *
     * @param lat1   vĩ độ điểm A
     * @param lon1   kinh độ điểm A
     * @param lat2   vĩ độ điểm B
     * @param lon2   kinh độ điểm B
     * @param buffer khoảng đệm (độ), mặc định 0.03 độ (~3km)
     * @return mảng [minLon, minLat, maxLon, maxLat]
     */
    public static double[] calculateBoundingBox(double lat1, double lon1,
                                                 double lat2, double lon2,
                                                 double buffer) {
        double minLon = Math.min(lon1, lon2) - buffer;
        double maxLon = Math.max(lon1, lon2) + buffer;
        double minLat = Math.min(lat1, lat2) - buffer;
        double maxLat = Math.max(lat1, lat2) + buffer;

        log.debug("[GeoUtils] BoundingBox: minLon={}, minLat={}, maxLon={}, maxLat={}",
                minLon, minLat, maxLon, maxLat);

        return new double[]{minLon, minLat, maxLon, maxLat};
    }

    /**
     * Tạo hình vuông (Polygon) bao quanh một điểm tâm.
     *
     * <p>Công thức tạo 4 góc hình vuông:
     * <ul>
     *   <li>Góc Tây Nam (SW): [lon - offset, lat - offset]</li>
     *   <li>Góc Đông Nam (SE): [lon + offset, lat - offset]</li>
     *   <li>Góc Đông Bắc (NE): [lon + offset, lat + offset]</li>
     *   <li>Góc Tây Bắc (NW): [lon - offset, lat + offset]</li>
     *   <li>Điểm đóng: [lon - offset, lat - offset] (trùng điểm đầu)</li>
     * </ul>
     *
     * <p>Lưu ý: Polygon phải khép kín (điểm cuối = điểm đầu).
     * Format: [[lon, lat], [lon, lat], ...]
     *
     * @param lat    vĩ độ tâm điểm ngập
     * @param lon    kinh độ tâm điểm ngập
     * @param offset khoảng lệch (độ), mặc định 0.002 độ (~200m)
     * @return danh sách tọa độ [lon, lat] tạo thành hình vuông khép kín
     */
    public static List<List<Double>> createSquarePolygon(double lat, double lon, double offset) {
        List<List<Double>> polygon = new ArrayList<>(5);

        // Góc Tây Nam (SW)
        polygon.add(List.of(lon - offset, lat - offset));

        // Góc Đông Nam (SE)
        polygon.add(List.of(lon + offset, lat - offset));

        // Góc Đông Bắc (NE)
        polygon.add(List.of(lon + offset, lat + offset));

        // Góc Tây Bắc (NW)
        polygon.add(List.of(lon - offset, lat + offset));

        // Điểm đóng (trùng điểm đầu)
        polygon.add(List.of(lon - offset, lat - offset));

        return polygon;
    }

    /**
     * Kiểm tra xem một điểm có nằm trong Bounding Box hay không.
     *
     * @param lat  vĩ độ điểm cần kiểm tra
     * @param lon  kinh độ điểm cần kiểm tra
     * @param bbox mảng [minLon, minLat, maxLon, maxLat]
     * @return true nếu điểm nằm trong bbox
     */
    public static boolean isPointInBoundingBox(double lat, double lon, double[] bbox) {
        return lon >= bbox[0] && lon <= bbox[2] && lat >= bbox[1] && lat <= bbox[3];
    }
}
