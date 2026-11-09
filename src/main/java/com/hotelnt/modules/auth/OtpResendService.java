package com.hotelnt.modules.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelnt.domain.OtpChallenge;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OtpResendService {
    private final AuthChallengeRepository challenges;
    private final OtpCodeGenerator codes;
    private final OtpHasher hashes;
    private final OtpEmailSender email;
    private final ObjectMapper json;

    OtpResendService(AuthChallengeRepository challenges, OtpCodeGenerator codes, OtpHasher hashes,
                     OtpEmailSender email, ObjectMapper json) {
        this.challenges = challenges; this.codes = codes; this.hashes = hashes; this.email = email; this.json = json;
    }
    enum Status { SUCCESS, EXPIRED, TOO_SOON, LIMIT }
    record Response(String challengeId, int otpExpiresIn, int resendAfter) {}
    record Result(Status status, Response response, Object details) {}

    @Transactional
    Result resend(OtpResendRequest request) {
        if (!email.isConfigured()) throw new IllegalStateException("SMTP is not configured");
        OtpChallenge c = challenges.findForUpdate(request.challengeId()).orElse(null);
        if (c == null || Boolean.TRUE.equals(c.getDaDung())) return new Result(Status.EXPIRED, null, null);
        ObjectNode data = metadata(c.getDuLieuDangKy());
        if (data.path("resetPending").asBoolean(false)) return new Result(Status.EXPIRED, null, null);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        long wait = OtpTiming.secondsUntilResend(c.getGuiLaiSauLuc(), now);
        if (wait > 0) return new Result(Status.TOO_SOON, null,
                List.of(Map.of("field", "retryAfter", "message", Long.toString(wait))));
        int count = data.path("resendCount").asInt(0);
        if (count >= 5) return new Result(Status.LIMIT, null, null);
        String code = codes.generate();
        data.put("resendCount", count + 1);
        c.setDuLieuDangKy(data.toString());
        c.setOtpHash(hashes.hash(code));
        c.setSoLanSai(0);
        c.setHetHanLuc(OtpTiming.expiresAt(now));
        c.setGuiLaiSauLuc(OtpTiming.resendAllowedAt(now));
        if (!data.path("fakeChallenge").asBoolean(false)) email.send(c.getEmail(), code);
        return new Result(Status.SUCCESS, new Response(c.getChallengeId(), OtpTiming.EXPIRES_IN_SECONDS,
                OtpTiming.RESEND_AFTER_SECONDS), null);
    }

    private ObjectNode metadata(String raw) {
        if (raw == null || raw.isBlank()) return json.createObjectNode();
        try {
            if (json.readTree(raw) instanceof ObjectNode node) return node;
            throw new IllegalStateException("Invalid OTP metadata");
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid OTP metadata", ex); }
    }
}
