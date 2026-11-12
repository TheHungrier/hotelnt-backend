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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshService {
    private final AuthTokenService tokens;
    private final AuthSessionRepository sessions;

    public RefreshService(AuthTokenService tokens, AuthSessionRepository sessions) {
        this.tokens = tokens;
        this.sessions = sessions;
    }

    public enum Status { SUCCESS, INVALID, ACCOUNT_LOCKED }
    public record Result(Status status, SessionIssuer.TokenPair pair) {}

    @Transactional
    public Result refresh(String refreshToken) {
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
