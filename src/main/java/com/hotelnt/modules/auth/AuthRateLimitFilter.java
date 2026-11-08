package com.hotelnt.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.common.error.ApiErrorResponse;
import com.hotelnt.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Gioi han theo IP va endpoint cho mot tien trinh; khong tin X-Forwarded-For tu client. */
@Component
class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_REQUESTS = 10;
    private static final int MAX_KEYS = 10_000;
    private static final long WINDOW_MS = 60_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    AuthRateLimitFilter(ObjectMapper json) { this.json = json; }
    private static final class Window { volatile long started; int count; Window(long started) { this.started = started; } }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod()) || !path.startsWith("/auth/")
                || path.equals("/auth/refresh") || path.equals("/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        if (windows.size() >= MAX_KEYS && !windows.containsKey(key)) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().started >= WINDOW_MS);
            if (windows.size() >= MAX_KEYS) { reject(response); return; }
        }
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        boolean allowed;
        synchronized (window) {
            if (now - window.started >= WINDOW_MS || now < window.started) { window.started = now; window.count = 0; }
            allowed = ++window.count <= MAX_REQUESTS;
        }
        if (!allowed) { reject(response); return; }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        ErrorCode error = ErrorCode.RATE_LIMITED;
        json.writeValue(response.getWriter(), ApiErrorResponse.of(error.name(), error.getMessage(), null));
    }
}
