package com.hotelnt.helper;

import com.hotelnt.domain.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionIssuer {
    private final AuthTokenService tokens;
    private final PhienDangNhapRepository sessions;

    public SessionIssuer(AuthTokenService tokens, PhienDangNhapRepository sessions) {
        this.tokens = tokens;
        this.sessions = sessions;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }

    public TokenPair issue(TaiKhoan account) {
        String id = UUID.randomUUID().toString();
        String refresh = tokens.refreshToken(account.getId(), id);
        PhienDangNhap session = new PhienDangNhap();
        session.setMaPhien(id);
        session.setTaiKhoan(account);
        session.setRefreshTokenHash(hash(refresh));
        session.setHetHanLuc(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(tokens.refreshSeconds()));
        sessions.save(session);
        return new TokenPair(tokens.accessToken(account.getId(), id), refresh, AuthTokenService.ACCESS_SECONDS);
    }

    public static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
