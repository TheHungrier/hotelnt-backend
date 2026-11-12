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
import jakarta.servlet.http.HttpServletRequest;

/**
 * Trích xuất IP thật của client, xử lý các trường hợp đứng sau Reverse Proxy (Nginx, Cloudflare, Load Balancer, FE proxy).
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolveClientIp(HttpServletRequest request) {
        // 1. Kiểm tra X-Forwarded-For: định dạng chuẩn "client, proxy1, proxy2"
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            for (String part : parts) {
                String ip = part.trim();
                if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip;
                }
            }
        }

        // 2. Kiểm tra X-Real-IP (thường dùng bởi Nginx reverse proxy)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }

        // 3. Kiểm tra CF-Connecting-IP (Cloudflare)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank() && !"unknown".equalsIgnoreCase(cfConnectingIp)) {
            return cfConnectingIp.trim();
        }

        // 4. Fallback về remote address mặc định
        return request.getRemoteAddr();
    }
}
