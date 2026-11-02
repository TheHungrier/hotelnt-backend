package com.hotelnt.modules.listing;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Khung nhin ban do: goc Tay Nam (sw) va Dong Bac (ne). */
public record MapBounds(double swLat, double swLng, double neLat, double neLng) {

    /** Doc "lat,lng" cua hai goc; sai dinh dang thi nem VALIDATION_ERROR. */
    public static MapBounds parse(String northEast, String southWest) {
        List<Map<String, String>> errors = new ArrayList<>();
        double[] ne = parseLatLng("boundsNorthEast", northEast, errors);
        double[] sw = parseLatLng("boundsSouthWest", southWest, errors);
        if (ne != null && sw != null && sw[0] > ne[0]) {
            errors.add(Map.of("field", "boundsSouthWest",
                    "message", "Vĩ độ góc Tây Nam không được lớn hơn góc Đông Bắc"));
        }
        if (!errors.isEmpty()) throw new AppException(ErrorCode.VALIDATION_ERROR, errors);
        return new MapBounds(sw[0], sw[1], ne[0], ne[1]);
    }

    private static double[] parseLatLng(String field, String value, List<Map<String, String>> errors) {
        try {
            String[] parts = value.split(",");
            if (parts.length != 2) throw new NumberFormatException();
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) throw new NumberFormatException();
            return new double[] {lat, lng};
        } catch (RuntimeException e) {
            errors.add(Map.of("field", field, "message", "Sai định dạng, cần dạng \"lat,lng\""));
            return null;
        }
    }
}
