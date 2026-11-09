package com.hotelnt.modules.auth;

import com.hotelnt.domain.OtpChallenge;
import com.hotelnt.domain.TaiKhoan;
import com.hotelnt.repository.OtpChallengeRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ForgotPasswordService {
    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordService.class);
    private final AuthAccountRepository accounts;
    private final OtpChallengeRepository challenges;
    private final OtpCodeGenerator codes;
    private final OtpHasher hashes;
    private final OtpEmailSender email;
    ForgotPasswordService(AuthAccountRepository accounts, OtpChallengeRepository challenges, OtpCodeGenerator codes,
                          OtpHasher hashes, OtpEmailSender email) {
        this.accounts = accounts; this.challenges = challenges; this.codes = codes; this.hashes = hashes; this.email = email;
    }
    record Response(String challengeId, int otpExpiresIn, int resendAfter, String message) {}

    @Transactional
    Response request(ForgotPasswordRequest request) {
        if (!email.isConfigured()) throw new IllegalStateException("SMTP is not configured");
        TaiKhoan account = accounts.findForUpdate(request.emailOrPhone()).orElse(null);
        boolean usable = account != null && "ACTIVE".equals(account.getTrangThai());
        String otp = codes.generate();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        OtpChallenge c = new OtpChallenge();
        c.setChallengeId(UUID.randomUUID().toString());
        c.setLoai("FORGOT_PASSWORD");
        c.setEmail(usable ? account.getEmail() : "invalid@invalid.invalid");
        c.setTaiKhoan(usable ? account : null);
        c.setDuLieuDangKy(usable ? "{}" : "{\"fakeChallenge\":true}");
        c.setOtpHash(hashes.hash(otp));
        c.setHetHanLuc(OtpTiming.expiresAt(now));
        c.setGuiLaiSauLuc(OtpTiming.resendAllowedAt(now));
        challenges.save(c);
        if (usable) {
            try { email.send(account.getEmail(), otp); }
            catch (MailException ex) { log.warn("Không thể gửi email khôi phục mật khẩu"); }
        }
        return new Response(c.getChallengeId(), OtpTiming.EXPIRES_IN_SECONDS, OtpTiming.RESEND_AFTER_SECONDS,
                "Nếu thông tin bạn nhập là chính xác, một mã OTP khôi phục đã được gửi đến thiết bị của bạn.");
    }
}
