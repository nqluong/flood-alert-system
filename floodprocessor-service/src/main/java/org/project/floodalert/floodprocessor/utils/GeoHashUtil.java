package org.project.floodalert.floodprocessor.utils;

/**
 * Utility đơn giản để tạo GeoHash từ tọa độ lat/lon.
 * Sử dụng cho anti-spam key generation.
 *
 * <p>GeoHash là chuỗi base32 mã hóa vị trí địa lý thành grid cells.
 * Độ dài 7 ký tự tương đương khoảng 153m x 153m.</p>
 */
public class GeoHashUtil {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int[] BITS = {16, 8, 4, 2, 1};

    /**
     * Tạo GeoHash với độ dài chỉ định từ tọa độ.
     *
     * @param lat Latitude (-90.0 đến 90.0)
     * @param lon Longitude (-180.0 đến 180.0)
     * @param precision Độ dài GeoHash (khuyến nghị: 7)
     * @return GeoHash string
     */
    public static String encode(double lat, double lon, int precision) {
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        StringBuilder geohash = new StringBuilder();
        boolean isEven = true;
        int bit = 0;
        int ch = 0;

        while (geohash.length() < precision) {
            double mid;
            if (isEven) {
                // Longitude
                mid = (lonRange[0] + lonRange[1]) / 2;
                if (lon > mid) {
                    ch |= BITS[bit];
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                // Latitude
                mid = (latRange[0] + latRange[1]) / 2;
                if (lat > mid) {
                    ch |= BITS[bit];
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }

            isEven = !isEven;

            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    /**
     * Tạo GeoHash với độ dài mặc định 7 ký tự (khoảng 153m precision).
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return GeoHash string độ dài 7
     */
    public static String encode(double lat, double lon) {
        return encode(lat, lon, 7);
    }
}
