package com.hotelnt.modules.auth;

import com.hotelnt.domain.PhienDangNhap;
import com.hotelnt.domain.TaiKhoan;
import com.hotelnt.repository.PhienDangNhapRepository;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticatedAccountService {
    private final AuthTokenService tokens;
    private final PhienDangNhapRepository sessions;

    public AuthenticatedAccountService(AuthTokenService tokens, PhienDangNhapRepository sessions) {
        this.tokens = tokens;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public Optional<Identity> resolve(String authorization, String expectedRole) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return Optional.empty();
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) return Optional.empty();

        Claims claims;
        try {
            claims = tokens.verify(token, "access");
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        String sessionId = claims.get("sid", String.class);
        Integer accountId;
        try {
            accountId = Integer.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        PhienDangNhap session = sessions.findById(sessionId).orElse(null);
        if (session == null || Boolean.TRUE.equals(session.getDaThuHoi())
                || session.getTaiKhoan() == null
                || !accountId.equals(session.getTaiKhoan().getId())
                || !"ACTIVE".equals(session.getTaiKhoan().getTrangThai())
                || !expectedRole.equals(session.getTaiKhoan().getVaiTro())) {
            return Optional.empty();
        }
        TaiKhoan account = session.getTaiKhoan();
        return Optional.of(new Identity(account.getId(), account.getEmail(), account.getVaiTro(), sessionId));
    }

    public record Identity(Integer accountId, String email, String role, String sessionId) {}
}
