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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetPasswordService {
    private final AuthChallengeRepository challenges;
    private final ResetTokenGenerator tokens;
    private final PasswordHasher passwords;
    private final SessionRevocationService sessions;
    private final ObjectMapper json;
    public ResetPasswordService(AuthChallengeRepository challenges, ResetTokenGenerator tokens, PasswordHasher passwords,
                         SessionRevocationService sessions, ObjectMapper json) {
        this.challenges = challenges; this.tokens = tokens; this.passwords = passwords; this.sessions = sessions; this.json = json;
    }

    @Transactional
    public boolean reset(ResetPasswordRequest request) {
        String id = tokens.challengeId(request.resetToken());
        if (id == null) return false;
        OtpChallenge c = challenges.findForUpdate(id).orElse(null);
        if (c == null || !"FORGOT_PASSWORD".equals(c.getLoai()) || Boolean.TRUE.equals(c.getDaDung())
                || c.getTaiKhoan() == null || !"ACTIVE".equals(c.getTaiKhoan().getTrangThai())
                || OtpTiming.isExpired(c.getHetHanLuc(), LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                || !pending(c.getDuLieuDangKy()) || !MessageDigest.isEqual(
                    SessionIssuer.hash(request.resetToken()).getBytes(StandardCharsets.US_ASCII),
                    c.getOtpHash().getBytes(StandardCharsets.US_ASCII))) return false;
        c.getTaiKhoan().setMatKhau(passwords.hash(request.newPassword()));
        c.setDaDung(true);
        c.setDuLieuDangKy(null);
        sessions.revokeAccountSessions(c.getTaiKhoan().getId());
        return true;
    }

    private boolean pending(String raw) {
        if (raw == null) return false;
        try { return json.readTree(raw).path("resetPending").asBoolean(false); }
        catch (JsonProcessingException ex) { return false; }
    }
}
