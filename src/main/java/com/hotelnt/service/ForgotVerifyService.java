package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForgotVerifyService {
    private final AuthChallengeRepository challenges;
    private final ChallengeVerifier verifier;
    private final ResetTokenGenerator tokens;
    private final ObjectMapper json;

    public ForgotVerifyService(AuthChallengeRepository challenges, ChallengeVerifier verifier,
            ResetTokenGenerator tokens, ObjectMapper json) {
        this.challenges = challenges;
        this.verifier = verifier;
        this.tokens = tokens;
        this.json = json;
    }

    public record Result(ChallengeVerifier.Result verification, String resetToken) {
    }

    @Transactional
    public Result verify(ChallengeVerifyRequest request) {
        OtpChallenge c = challenges.findForUpdate(request.challengeId()).orElse(null);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        ObjectNode data = c == null ? json.createObjectNode() : metadata(c.getDuLieuDangKy());
        if (data.path("resetPending").asBoolean(false))
            return new Result(new ChallengeVerifier.Result(ChallengeVerifier.Status.EXPIRED, null), null);
        ChallengeVerifier.Result checked = verifier.check(c, "FORGOT_PASSWORD", request.otpCode(), now);
        if (checked.status() != ChallengeVerifier.Status.VALID)
            return new Result(checked, null);
        if (c.getTaiKhoan() == null || !"ACTIVE".equals(c.getTaiKhoan().getTrangThai()))
            return new Result(new ChallengeVerifier.Result(ChallengeVerifier.Status.EXPIRED, null), null);
        String token = tokens.generate(c.getChallengeId());
        c.setOtpHash(SessionIssuer.hash(token));
        c.setHetHanLuc(now.plusMinutes(10));
        data.put("resetPending", true);
        c.setDuLieuDangKy(data.toString());
        return new Result(checked, token);
    }

    private ObjectNode metadata(String raw) {
        if (raw == null || raw.isBlank())
            return json.createObjectNode();
        try {
            if (json.readTree(raw) instanceof ObjectNode node)
                return node;
            throw new IllegalStateException("Invalid OTP metadata");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid OTP metadata", ex);
        }
    }
}
