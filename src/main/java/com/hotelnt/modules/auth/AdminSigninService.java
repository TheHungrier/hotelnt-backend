package com.hotelnt.modules.auth;

import com.hotelnt.domain.TaiKhoan;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminSigninService {
    private final CredentialsChecker credentials;
    private final SessionIssuer sessions;
    AdminSigninService(CredentialsChecker credentials, SessionIssuer sessions) { this.credentials = credentials; this.sessions = sessions; }
    record UserInfo(Integer id, String hoTen, String email, String vaiTro) {}
    record Response(String accessToken, String refreshToken, long expiresIn, UserInfo userInfo) {}
    record Result(CredentialsChecker.Status status, Response response, Object details) {}

    @Transactional
    Result signin(AdminSigninRequest request) {
        CredentialsChecker.Result checked = credentials.check(request.email(), request.matKhau(), "ADMIN",
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        if (checked.status() != CredentialsChecker.Status.SUCCESS) return new Result(checked.status(), null, checked.details());
        TaiKhoan account = checked.account();
        SessionIssuer.TokenPair pair = sessions.issue(account);
        return new Result(checked.status(), new Response(pair.accessToken(), pair.refreshToken(), pair.expiresIn(),
                new UserInfo(account.getId(), account.getHoTen(), account.getEmail(), account.getVaiTro())), null);
    }
}
