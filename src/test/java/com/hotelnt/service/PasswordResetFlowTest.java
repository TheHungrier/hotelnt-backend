package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetFlowTest {
    @Test
    void resetTokenIsSingleUseAndRevokesSessions() {
        AuthChallengeRepository challenges = mock(AuthChallengeRepository.class);
        ChallengeVerifier verifier = mock(ChallengeVerifier.class);
        PasswordHasher passwords = mock(PasswordHasher.class);
        SessionRevocationService sessions = mock(SessionRevocationService.class);
        ResetTokenGenerator tokens = new ResetTokenGenerator();
        ObjectMapper json = new ObjectMapper();
        String id = "2b98e997-2423-410b-9ad4-4a888ef60571";
        TaiKhoan account = new TaiKhoan(); account.setId(42); account.setTrangThai("ACTIVE");
        OtpChallenge c = new OtpChallenge(); c.setChallengeId(id); c.setLoai("FORGOT_PASSWORD"); c.setTaiKhoan(account);
        c.setDuLieuDangKy("{}"); c.setHetHanLuc(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(5));
        when(challenges.findForUpdate(id)).thenReturn(Optional.of(c));
        when(verifier.check(org.mockito.ArgumentMatchers.eq(c), org.mockito.ArgumentMatchers.eq("FORGOT_PASSWORD"),
                org.mockito.ArgumentMatchers.eq("123456"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ChallengeVerifier.Result(ChallengeVerifier.Status.VALID, null));
        ForgotVerifyService verifyOtp = new ForgotVerifyService(challenges, verifier, tokens, json);
        String token = verifyOtp.verify(new ChallengeVerifyRequest(id, "123456")).resetToken();
        when(passwords.hash("NewPass123!")).thenReturn("new-bcrypt-hash");
        ResetPasswordService reset = new ResetPasswordService(challenges, tokens, passwords, sessions, json);
        ResetPasswordRequest request = new ResetPasswordRequest(token, "NewPass123!");
        assertTrue(reset.reset(request));
        assertFalse(reset.reset(request));
        verify(sessions).revokeAccountSessions(42);
    }
}