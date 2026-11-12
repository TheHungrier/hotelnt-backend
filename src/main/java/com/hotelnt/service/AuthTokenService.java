package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Tao va kiem tra JWT. Khoa ky phai duoc cau hinh ben ngoai source code. */
@Component
public class AuthTokenService {
    public static final long ACCESS_SECONDS = 1800;
    private final String secret;
    private final long refreshSeconds;

    public AuthTokenService(@Value("${app.auth.jwt-secret:}") String secret,
                     @Value("${app.auth.refresh-seconds:604800}") long refreshSeconds) {
        this.secret = secret;
        if (refreshSeconds <= 0) throw new IllegalArgumentException("Invalid refresh token lifetime");
        this.refreshSeconds = refreshSeconds;
    }

    public String accessToken(int accountId, String sessionId) {
        return issue(accountId, sessionId, "access", ACCESS_SECONDS);
    }

    public String refreshToken(int accountId, String sessionId) {
        return issue(accountId, sessionId, "refresh", refreshSeconds);
    }

    public long refreshSeconds() { return refreshSeconds; }

    public Claims verify(String token, String expectedType) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Missing token");
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get("type", String.class))
                    || claims.getSubject() == null || claims.get("sid", String.class) == null
                    || claims.getId() == null || claims.getExpiration() == null) {
                throw new IllegalArgumentException("Invalid token claims");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid token", ex);
        }
    }

    private String issue(int accountId, String sessionId, String type, long lifetimeSeconds) {
        if (accountId <= 0 || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Invalid token subject");
        }
        Instant now = Instant.now();
        return Jwts.builder().subject(Integer.toString(accountId)).id(UUID.randomUUID().toString())
                .claim("sid", sessionId).claim("type", type).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(lifetimeSeconds))).signWith(key()).compact();
    }

    private SecretKey key() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("APP_AUTH_JWT_SECRET must contain at least 32 bytes");
        return Keys.hmacShaKeyFor(bytes);
    }
}
