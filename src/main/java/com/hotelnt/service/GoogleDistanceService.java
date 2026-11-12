package com.hotelnt.service;

import com.hotelnt.helper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tính khoảng cách địa lý giữa người dùng và cơ sở lưu trú theo tuyến đường
 * thực tế trên Google Maps
 * (Google Maps Distance Matrix API), thay vì tính theo đường chim bay
 * (Haversine).
 * Có cơ chế Cache để tối ưu hiệu năng và chi phí API, cùng cơ chế fallback an
 * toàn về công thức hình học.
 */
@Service
public class GoogleDistanceService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDistanceService.class);
    private static final String DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    private final ObjectMapper json;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, Double> cache = new ConcurrentHashMap<>();

    @Value("${app.google.maps-api-key:${GOOGLE_MAPS_API_KEY:}}")
    private String apiKey;

    public GoogleDistanceService(ObjectMapper json) {
        this.json = json;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public GoogleDistanceService(ObjectMapper json, HttpClient httpClient, String apiKey) {
        this.json = json;
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    /**
     * Tính khoảng cách km (làm tròn 2 chữ số thập phân).
     * 
     * @return Khoảng cách thực tế theo Google Maps, hoặc fallback theo Haversine
     *         nếu thiếu API key/lỗi mạng.
     */
    public Double calculateDistanceKm(Double originLat, Double originLng, Double destLat, Double destLng) {
        if (originLat == null || originLng == null || destLat == null || destLng == null) {
            return null;
        }

        if (Double.compare(originLat, destLat) == 0 && Double.compare(originLng, destLng) == 0) {
            return 0.0;
        }

        // Cache key theo tọa độ làm tròn 4 chữ số thập phân (~11m sai số)
        String cacheKey = String.format(Locale.US, "%.4f,%.4f->%.4f,%.4f", originLat, originLng, destLat, destLng);
        Double cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String key = resolveApiKey();
        if (key == null || key.isBlank()) {
            double fallback = round(GeoUtils.haversineDistanceKm(originLat, originLng, destLat, destLng));
            cache.put(cacheKey, fallback);
            return fallback;
        }

        try {
            String url = String.format(Locale.US,
                    "%s?origins=%.6f,%.6f&destinations=%.6f,%.6f&mode=driving&units=metric&key=%s",
                    DISTANCE_MATRIX_URL, originLat, originLng, destLat, destLng,
                    URLEncoder.encode(key, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = json.readTree(response.body());
                String status = root.path("status").asText();
                if ("OK".equals(status)) {
                    JsonNode element = root.path("rows").path(0).path("elements").path(0);
                    String elementStatus = element.path("status").asText();
                    if ("OK".equals(elementStatus)) {
                        long meters = element.path("distance").path("value").asLong();
                        double km = round(meters / 1000.0);
                        cache.put(cacheKey, km);
                        return km;
                    }
                }
                log.warn("Google Distance Matrix API trả về trạng thái không thành công: {}", status);
            } else {
                log.warn("Google Distance Matrix API HTTP status lỗi: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi kết nối Google Distance Matrix API: {}", e.getMessage());
        }

        double fallback = round(GeoUtils.haversineDistanceKm(originLat, originLng, destLat, destLng));
        cache.put(cacheKey, fallback);
        return fallback;
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String envKey = System.getenv("GOOGLE_MAPS_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }

    private static double round(double km) {
        return Math.round(km * 100.0) / 100.0;
    }
}
