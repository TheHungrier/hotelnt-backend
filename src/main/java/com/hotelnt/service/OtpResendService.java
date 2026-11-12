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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OtpResendService {
    private static final Logger log = LoggerFactory.getLogger(OtpResendService.class);
    private final AuthChallengeRepository challenges;
    private final OtpCodeGenerator codes;
    private final OtpHasher hashes;
    private final OtpEmailSender email;
    private final ObjectMapper json;

    public OtpResendService(AuthChallengeRepository challenges, OtpCodeGenerator codes, OtpHasher hashes,
                     OtpEmailSender email, ObjectMapper json) {
        this.challenges = challenges; this.codes = codes; this.hashes = hashes; this.email = email; this.json = json;
    }
    public enum Status { SUCCESS, EXPIRED, TOO_SOON, LIMIT }
    public record Response(String challengeId, int otpExpiresIn, int resendAfter) {}
    public record Result(Status status, Response response, Object details) {}

    @Transactional
    public Result resend(OtpResendRequest request) {
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
        if (!data.path("fakeChallenge").asBoolean(false)) {
            sendAfterCommit(c.getEmail(), code);
        }
        return new Result(Status.SUCCESS, new Response(c.getChallengeId(), OtpTiming.EXPIRES_IN_SECONDS,
                OtpTiming.RESEND_AFTER_SECONDS), null);
    }

    private void sendAfterCommit(String recipient, String code) {
        Runnable send = () -> {
            try {
                email.send(recipient, code);
            } catch (RuntimeException ex) {
                log.warn("Không thể gửi lại email OTP cho challenge", ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private ObjectNode metadata(String raw) {
        if (raw == null || raw.isBlank()) return json.createObjectNode();
        try {
            if (json.readTree(raw) instanceof ObjectNode node) return node;
            throw new IllegalStateException("Invalid OTP metadata");
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid OTP metadata", ex); }
    }
}
