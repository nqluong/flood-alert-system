package org.project.floodalert.floodcore.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void testCalculateBoundingBox() {
        // Điểm A: (10.776, 106.700)
        // Điểm B: (10.780, 106.710)
        // Buffer: 0.03
        double[] bbox = GeoUtils.calculateBoundingBox(10.776, 106.700, 10.780, 106.710, 0.03);

        // Kỳ vọng: [minLon, minLat, maxLon, maxLat]
        assertEquals(106.670, bbox[0], 0.001); // minLon = 106.700 - 0.03
        assertEquals(10.746, bbox[1], 0.001);  // minLat = 10.776 - 0.03
        assertEquals(106.740, bbox[2], 0.001); // maxLon = 106.710 + 0.03
        assertEquals(10.810, bbox[3], 0.001);  // maxLat = 10.780 + 0.03
    }

    @Test
    void testCreateSquarePolygon() {
        // Tâm điểm: (10.776, 106.700)
        // Offset: 0.002
        List<List<Double>> polygon = GeoUtils.createSquarePolygon(10.776, 106.700, 0.002);

        // Polygon phải có 5 điểm (4 góc + 1 điểm đóng)
        assertEquals(5, polygon.size());

        // Điểm đầu và điểm cuối phải trùng nhau (khép kín)
        assertEquals(polygon.get(0), polygon.get(4));

        // Kiểm tra góc Tây Nam (SW)
        assertEquals(106.698, polygon.get(0).get(0), 0.001); // lon - offset
        assertEquals(10.774, polygon.get(0).get(1), 0.001);  // lat - offset

        // Kiểm tra góc Đông Bắc (NE)
        assertEquals(106.702, polygon.get(2).get(0), 0.001); // lon + offset
        assertEquals(10.778, polygon.get(2).get(1), 0.001);  // lat + offset
    }

    @Test
    void testIsPointInBoundingBox() {
        double[] bbox = new double[]{106.670, 10.746, 106.740, 10.810};

        // Điểm nằm trong bbox
        assertTrue(GeoUtils.isPointInBoundingBox(10.776, 106.700, bbox));

        // Điểm nằm ngoài bbox
        assertFalse(GeoUtils.isPointInBoundingBox(10.900, 106.700, bbox));
        assertFalse(GeoUtils.isPointInBoundingBox(10.776, 106.800, bbox));
    }
}
