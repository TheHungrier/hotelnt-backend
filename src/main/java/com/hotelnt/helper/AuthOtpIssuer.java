package com.hotelnt.helper;

import com.hotelnt.domain.*;
import com.hotelnt.repository.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuthOtpIssuer {
    private final OtpCodeGenerator codes;
    private final OtpHasher hashes;
    private final OtpChallengeRepository challenges;
    private final OtpEmailSender email;

    public AuthOtpIssuer(OtpCodeGenerator codes, OtpHasher hashes, OtpChallengeRepository challenges,
            OtpEmailSender email) {
        this.codes = codes;
        this.hashes = hashes;
        this.challenges = challenges;
        this.email = email;
    }

    public String issue(TaiKhoan account, String type, LocalDateTime now) {
        String code = codes.generate();
        OtpChallenge challenge = new OtpChallenge();
        challenge.setChallengeId(UUID.randomUUID().toString());
        challenge.setLoai(type);
        challenge.setEmail(account.getEmail());
        challenge.setTaiKhoan(account);
        challenge.setOtpHash(hashes.hash(code));
        challenge.setHetHanLuc(OtpTiming.expiresAt(now));
        challenge.setGuiLaiSauLuc(OtpTiming.resendAllowedAt(now));
        challenges.save(challenge);
        email.send(account.getEmail(), code);
        return challenge.getChallengeId();
    }
}
