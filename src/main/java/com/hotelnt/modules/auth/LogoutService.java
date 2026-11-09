package com.hotelnt.modules.auth;

import com.hotelnt.domain.PhienDangNhap;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LogoutService {
    private final AuthTokenService tokens;
    private final AuthSessionRepository sessions;

    LogoutService(AuthTokenService tokens, AuthSessionRepository sessions) {
        this.tokens = tokens;
        this.sessions = sessions;
    }

    @Transactional
    boolean logout(String bearer, String refreshToken) {
        if (bearer == null || !bearer.startsWith("Bearer ")) return false;
        Claims access;
        Claims refresh;
        try {
            access = tokens.verify(bearer.substring(7), "access");
            refresh = tokens.verify(refreshToken, "refresh");
        } catch (IllegalArgumentException ex) { return false; }
        if (!access.getSubject().equals(refresh.getSubject())
                || !access.get("sid", String.class).equals(refresh.get("sid", String.class))) return false;
        PhienDangNhap session = sessions.findForUpdate(refresh.get("sid", String.class)).orElse(null);
        if (session == null || !Integer.toString(session.getTaiKhoan().getId()).equals(refresh.getSubject())
                || !LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).isBefore(session.getHetHanLuc())
                || !MessageDigest.isEqual(SessionIssuer.hash(refreshToken).getBytes(StandardCharsets.US_ASCII),
                    session.getRefreshTokenHash().getBytes(StandardCharsets.US_ASCII))) return false;
        session.setDaThuHoi(true);
        return true;
    }
}
