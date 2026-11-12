package com.hotelnt.common;

public final class GeoUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {}

    /**
     * Khoảng cách (km) giữa hai tọa độ theo công thức Haversine (đường chim bay).
     * Dùng cho tính toán hình học hoặc fallback an toàn khi không có kết nối Google Maps API.
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Giữ nguyên để tương thích ngược với các thành phần cũ. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        return haversineDistanceKm(lat1, lon1, lat2, lon2);
    }
}
