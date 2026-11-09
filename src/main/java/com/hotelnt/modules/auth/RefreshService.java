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
class RefreshService {
    private final AuthTokenService tokens;
    private final AuthSessionRepository sessions;

    RefreshService(AuthTokenService tokens, AuthSessionRepository sessions) {
        this.tokens = tokens;
        this.sessions = sessions;
    }

    enum Status { SUCCESS, INVALID, ACCOUNT_LOCKED }
    record Result(Status status, SessionIssuer.TokenPair pair) {}

    @Transactional
    Result refresh(String refreshToken) {
        Claims claims;
        try { claims = tokens.verify(refreshToken, "refresh"); }
        catch (IllegalArgumentException ex) { return new Result(Status.INVALID, null); }
        PhienDangNhap session = sessions.findForUpdate(claims.get("sid", String.class)).orElse(null);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (session == null || !Integer.toString(session.getTaiKhoan().getId()).equals(claims.getSubject())
                || Boolean.TRUE.equals(session.getDaThuHoi()) || !now.isBefore(session.getHetHanLuc())) {
            return new Result(Status.INVALID, null);
        }
        if (!MessageDigest.isEqual(SessionIssuer.hash(refreshToken).getBytes(StandardCharsets.US_ASCII),
                session.getRefreshTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            session.setDaThuHoi(true);
            return new Result(Status.INVALID, null);
        }
        if (!"ACTIVE".equals(session.getTaiKhoan().getTrangThai())) {
            return new Result(Status.ACCOUNT_LOCKED, null);
        }
        int accountId = session.getTaiKhoan().getId();
        String refresh = tokens.refreshToken(accountId, session.getMaPhien());
        session.setRefreshTokenHash(SessionIssuer.hash(refresh));
        session.setHetHanLuc(now.plusSeconds(tokens.refreshSeconds()));
        return new Result(Status.SUCCESS, new SessionIssuer.TokenPair(
                tokens.accessToken(accountId, session.getMaPhien()), refresh, AuthTokenService.ACCESS_SECONDS));
    }
}
