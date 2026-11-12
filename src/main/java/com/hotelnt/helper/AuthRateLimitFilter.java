package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.common.error.ApiErrorResponse;
import com.hotelnt.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bo loc gioi han toc do (Rate Limit):
 * - Tach rieng theo IP (xu ly dung qua Reverse Proxy) va theo Token (Access / Refresh token).
 * - Tranh ban oan nguoi dung trong cung mang NAT / cung endpoint / di qua reverse proxy cua FE.
 * - Co rate limit rieng cho Refresh token de chong bypass va spam endpoint /auth/refresh.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    static final int MAX_IP_REQUESTS = 10;
    static final int MAX_TOKEN_REQUESTS = 30;
    static final int MAX_REFRESH_REQUESTS = 10;
    static final int MAX_REFRESH_IP_BURST = 30;
    private static final int MAX_KEYS = 10_000;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper json;

    AuthRateLimitFilter(ObjectMapper json) {
        this.json = json;
    }

    private static final class Window {
        volatile long started;
        int count;

        Window(long started) {
            this.started = started;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // Khong bo qua /auth/refresh nua de chong bypass; chi bo qua ngoai POST /auth/* va /auth/logout
        return !"POST".equals(request.getMethod()) || !path.startsWith("/auth/")
                || path.equals("/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String clientIp = ClientIpResolver.resolveClientIp(request);
        HttpServletRequest requestToUse = request;

        long now = System.currentTimeMillis();

        if (path.equals("/auth/refresh")) {
            // Rate limit rieng cho endpoint /auth/refresh
            String refreshToken = extractTokenFromHeader(request);

            boolean isJson = request.getContentType() != null && request.getContentType().contains("application/json");
            if (isJson) {
                CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);
                requestToUse = cached;
                if (refreshToken == null) {
                    refreshToken = extractRefreshTokenFromBody(cached.getCachedBody());
                }
            }

            if (refreshToken != null && !refreshToken.isBlank()) {
                // Rate limit theo token rieng
                String tokenKey = "TOKEN:REFRESH:" + hash(refreshToken);
                if (!checkRateLimit(tokenKey, MAX_REFRESH_REQUESTS, now)) {
                    reject(response);
                    return;
                }
                // Kem theo gioi han tong IP cho refresh de phong spam nhieu token gia tu 1 IP
                String ipBurstKey = "IP:REFRESH_ALL:" + clientIp;
                if (!checkRateLimit(ipBurstKey, MAX_REFRESH_IP_BURST, now)) {
                    reject(response);
                    return;
                }
            } else {
                // Khong co token (hoac body rong / sai format): gioi han theo IP
                String ipKey = "IP:REFRESH:" + clientIp;
                if (!checkRateLimit(ipKey, MAX_IP_REQUESTS, now)) {
                    reject(response);
                    return;
                }
            }
        } else {
            // Cac endpoint POST /auth/* khac
            String bearerToken = extractTokenFromHeader(request);
            if (bearerToken != null && !bearerToken.isBlank()) {
                // Nguoi dung da co token: rate limit theo token rieng, khong anh huong nguoi khac cung IP
                String tokenKey = "TOKEN:" + path + ":" + hash(bearerToken);
                if (!checkRateLimit(tokenKey, MAX_TOKEN_REQUESTS, now)) {
                    reject(response);
                    return;
                }
            } else {
                // Chua co token (anonymous): rate limit theo IP that cua client
                String ipKey = "IP:" + path + ":" + clientIp;
                if (!checkRateLimit(ipKey, MAX_IP_REQUESTS, now)) {
                    reject(response);
                    return;
                }
            }
        }

        chain.doFilter(requestToUse, response);
    }

    private boolean checkRateLimit(String key, int maxRequests, long now) {
        if (windows.size() >= MAX_KEYS && !windows.containsKey(key)) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().started >= WINDOW_MS);
            if (windows.size() >= MAX_KEYS) {
                return false;
            }
        }
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.started >= WINDOW_MS || now < window.started) {
                window.started = now;
                window.count = 0;
            }
            return ++window.count <= maxRequests;
        }
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    private String extractRefreshTokenFromBody(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            JsonNode tree = json.readTree(body);
            if (tree != null && tree.has("refreshToken")) {
                String token = tree.get("refreshToken").asText();
                return (token != null && !token.isBlank()) ? token.trim() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        ErrorCode error = ErrorCode.RATE_LIMITED;
        json.writeValue(response.getWriter(), ApiErrorResponse.of(error.name(), error.getMessage(), null));
    }

    private static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
