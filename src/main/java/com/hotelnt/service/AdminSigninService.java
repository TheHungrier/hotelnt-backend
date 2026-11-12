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
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSigninService {
    private final CredentialsChecker credentials;
    private final SessionIssuer sessions;
    public AdminSigninService(CredentialsChecker credentials, SessionIssuer sessions) { this.credentials = credentials; this.sessions = sessions; }
    public record UserInfo(Integer id, String hoTen, String email, String vaiTro) {}
    public record Response(String accessToken, String refreshToken, long expiresIn, UserInfo userInfo) {}
    public record Result(CredentialsChecker.Status status, Response response, Object details) {}

    @Transactional
    public Result signin(AdminSigninRequest request) {
        CredentialsChecker.Result checked = credentials.check(request.email(), request.matKhau(), "ADMIN",
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        if (checked.status() != CredentialsChecker.Status.SUCCESS) return new Result(checked.status(), null, checked.details());
        TaiKhoan account = checked.account();
        SessionIssuer.TokenPair pair = sessions.issue(account);
        return new Result(checked.status(), new Response(pair.accessToken(), pair.refreshToken(), pair.expiresIn(),
                new UserInfo(account.getId(), account.getHoTen(), account.getEmail(), account.getVaiTro())), null);
    }
}
