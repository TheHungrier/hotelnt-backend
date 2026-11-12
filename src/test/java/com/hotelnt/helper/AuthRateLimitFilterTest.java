package com.hotelnt.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    @Test
    void limitsRepeatedRequestsByAddressAndEndpoint() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/signin");
            request.setContextPath("/api");
            request.setRemoteAddr("127.0.0.1");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/signin");
        request.setContextPath("/api");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("RATE_LIMITED"));
        verify(chain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void supportsReverseProxyWithForwardedHeaders() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        // Client 1 sau Reverse Proxy (remoteAddr la 10.0.0.1 cua proxy, nhung
        // X-Forwarded-For la 1.1.1.1)
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/signin");
            req.setContextPath("/api");
            req.setRemoteAddr("10.0.0.1");
            req.addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // Lan thu 11 cua Client 1 bi 429
        MockHttpServletRequest req1Blocked = new MockHttpServletRequest("POST", "/api/auth/signin");
        req1Blocked.setContextPath("/api");
        req1Blocked.setRemoteAddr("10.0.0.1");
        req1Blocked.addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(req1Blocked, res1, chain);
        assertEquals(429, res1.getStatus());

        // Client 2 cung di qua Reverse Proxy (remoteAddr 10.0.0.1, nhung
        // X-Forwarded-For la 2.2.2.2) KHONG BI BAN OAN!
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/auth/signin");
        req2.setContextPath("/api");
        req2.setRemoteAddr("10.0.0.1");
        req2.addHeader("X-Forwarded-For", "2.2.2.2, 10.0.0.1");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, chain);
        assertEquals(200, res2.getStatus());
    }

    @Test
    void separatesRateLimitByBearerToken() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        // Hai user cung IP 192.168.1.10 (cung mang noi bo)
        String userTokenA = "token-user-a-secret-jwt";
        String userTokenB = "token-user-b-secret-jwt";

        // User A goi toi da 30 requests voi token cua minh
        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/otp/resend");
            req.setContextPath("/api");
            req.setRemoteAddr("192.168.1.10");
            req.addHeader("Authorization", "Bearer " + userTokenA);
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // Lan thu 31 cua User A bi rate limit
        MockHttpServletRequest reqABlocked = new MockHttpServletRequest("POST", "/api/auth/otp/resend");
        reqABlocked.setContextPath("/api");
        reqABlocked.setRemoteAddr("192.168.1.10");
        reqABlocked.addHeader("Authorization", "Bearer " + userTokenA);
        MockHttpServletResponse resA = new MockHttpServletResponse();
        filter.doFilter(reqABlocked, resA, chain);
        assertEquals(429, resA.getStatus());

        // User B cung IP nhung co token rieng -> KHONG BI BAN OAN!
        MockHttpServletRequest reqB = new MockHttpServletRequest("POST", "/api/auth/otp/resend");
        reqB.setContextPath("/api");
        reqB.setRemoteAddr("192.168.1.10");
        reqB.addHeader("Authorization", "Bearer " + userTokenB);
        MockHttpServletResponse resB = new MockHttpServletResponse();
        filter.doFilter(reqB, resB, chain);
        assertEquals(200, resB.getStatus());
    }

    @Test
    void rateLimitsRefreshTokenSpam() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        String refreshPayload = "{\"refreshToken\":\"refresh-token-xyz-123\"}";

        // Spam 10 lan endpoint /auth/refresh voi cung refresh token
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
            req.setContextPath("/api");
            req.setContentType("application/json");
            req.setContent(refreshPayload.getBytes());
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // Lan thu 11 bi 429
        MockHttpServletRequest req11 = new MockHttpServletRequest("POST", "/api/auth/refresh");
        req11.setContextPath("/api");
        req11.setContentType("application/json");
        req11.setContent(refreshPayload.getBytes());
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req11, res, chain);

        assertEquals(429, res.getStatus());
        assertTrue(res.getContentAsString().contains("RATE_LIMITED"));
    }

    @Test
    void separatesRefreshTokenRateLimitBetweenUsersOnSameIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        String tokenA = "{\"refreshToken\":\"user-a-refresh-token\"}";
        String tokenB = "{\"refreshToken\":\"user-b-refresh-token\"}";

        // User A spam 10 lan
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
            req.setContextPath("/api");
            req.setRemoteAddr("192.168.1.50");
            req.setContentType("application/json");
            req.setContent(tokenA.getBytes());
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // User B cung IP 192.168.1.50 dung token cua minh van refresh duoc binh thuong
        MockHttpServletRequest reqB = new MockHttpServletRequest("POST", "/api/auth/refresh");
        reqB.setContextPath("/api");
        reqB.setRemoteAddr("192.168.1.50");
        reqB.setContentType("application/json");
        reqB.setContent(tokenB.getBytes());
        MockHttpServletResponse resB = new MockHttpServletResponse();
        filter.doFilter(reqB, resB, chain);

        assertEquals(200, resB.getStatus());
    }

    @Test
    void rateLimitsRefreshTokenWithoutBodyByIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper());
        FilterChain chain = mock(FilterChain.class);

        // Spam request /auth/refresh khong co token (empty body)
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
            req.setContextPath("/api");
            req.setRemoteAddr("123.45.67.89");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest req11 = new MockHttpServletRequest("POST", "/api/auth/refresh");
        req11.setContextPath("/api");
        req11.setRemoteAddr("123.45.67.89");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req11, res, chain);

        assertEquals(429, res.getStatus());
    }
}