package com.hotelnt.modules.auth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class HostSigninService {
    private final CredentialsChecker credentials;
    private final AuthOtpIssuer otp;
    HostSigninService(CredentialsChecker credentials, AuthOtpIssuer otp) { this.credentials = credentials; this.otp = otp; }
    record Response(String challengeId, int otpExpiresIn, int resendAfter) {}
    record Result(CredentialsChecker.Status status, Response response, Object details) {}

    @Transactional
    Result signin(HostSigninRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        CredentialsChecker.Result checked = credentials.check(request.loginIdentity(), request.matKhau(), "HOST", now);
        if (checked.status() != CredentialsChecker.Status.SUCCESS) return new Result(checked.status(), null, checked.details());
        String id = otp.issue(checked.account(), "SIGNIN", now);
        return new Result(checked.status(), new Response(id, OtpTiming.EXPIRES_IN_SECONDS, OtpTiming.RESEND_AFTER_SECONDS), null);
    }
}
