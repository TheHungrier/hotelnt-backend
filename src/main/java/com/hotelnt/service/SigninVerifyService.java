package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SigninVerifyService {
    private final AuthChallengeRepository challenges;
    private final ChallengeVerifier verifier;
    private final SessionIssuer sessions;

    public SigninVerifyService(AuthChallengeRepository challenges, ChallengeVerifier verifier, SessionIssuer sessions) {
        this.challenges = challenges;
        this.verifier = verifier;
        this.sessions = sessions;
    }

    public record Result(ChallengeVerifier.Result verification, boolean accountLocked,
            SignupVerificationService.Response response) {
    }

    @Transactional
    public Result verify(ChallengeVerifyRequest request) {
        OtpChallenge challenge = challenges.findForUpdate(request.challengeId()).orElse(null);
        ChallengeVerifier.Result checked = verifier.check(challenge, "SIGNIN", request.otpCode(),
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        if (checked.status() != ChallengeVerifier.Status.VALID)
            return new Result(checked, false, null);
        TaiKhoan account = challenge.getTaiKhoan();
        if (account == null || !"ACTIVE".equals(account.getTrangThai()) || !"HOST".equals(account.getVaiTro())) {
            return new Result(checked, true, null);
        }
        SessionIssuer.TokenPair pair = sessions.issue(account);
        challenge.setDaDung(true);
        return new Result(checked, false,
                new SignupVerificationService.Response(pair.accessToken(), pair.refreshToken(),
                        pair.expiresIn(), new SignupVerificationService.UserInfo(account.getId(), account.getHoTen(),
                                account.getEmail(), account.getSoDienThoai(), account.getVaiTro())));
    }
}
