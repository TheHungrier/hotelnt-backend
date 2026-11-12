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
public class HostSigninService {
    private final CredentialsChecker credentials;
    private final AuthOtpIssuer otp;
    public HostSigninService(CredentialsChecker credentials, AuthOtpIssuer otp) { this.credentials = credentials; this.otp = otp; }
    public record Response(String challengeId, int otpExpiresIn, int resendAfter) {}
    public record Result(CredentialsChecker.Status status, Response response, Object details) {}

    @Transactional
    public Result signin(HostSigninRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        CredentialsChecker.Result checked = credentials.check(request.loginIdentity(), request.matKhau(), "HOST", now);
        if (checked.status() != CredentialsChecker.Status.SUCCESS) return new Result(checked.status(), null, checked.details());
        String id = otp.issue(checked.account(), "SIGNIN", now);
        return new Result(checked.status(), new Response(id, OtpTiming.EXPIRES_IN_SECONDS, OtpTiming.RESEND_AFTER_SECONDS), null);
    }
}
