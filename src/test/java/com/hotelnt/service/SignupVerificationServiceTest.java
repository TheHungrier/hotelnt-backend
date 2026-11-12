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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SignupVerificationServiceTest {
    private final AuthChallengeRepository challenges = mock(AuthChallengeRepository.class);
    private final OtpHasher otpHasher = mock(OtpHasher.class);
    private final SignupDuplicateChecker duplicates = mock(SignupDuplicateChecker.class);
    private final TaiKhoanRepository accounts = mock(TaiKhoanRepository.class);
    private final SessionIssuer sessions = mock(SessionIssuer.class);
    private final ObjectMapper json = new ObjectMapper();
    private final SignupVerificationService service = new SignupVerificationService(challenges, otpHasher,
            duplicates, accounts, sessions, json);
    private final SignupVerifyRequest request = new SignupVerifyRequest("id", "123456");

    private OtpChallenge challenge() {
        OtpChallenge c = new OtpChallenge();
        c.setLoai("SIGNUP");
        c.setOtpHash("hash");
        c.setHetHanLuc(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(5));
        return c;
    }

    @Test
    void rejectsMissingChallenge() {
        when(challenges.findForUpdate("id")).thenReturn(Optional.empty());
        assertEquals(SignupVerificationService.Status.EXPIRED, service.verify(request).status());
    }

    @Test
    void countsWrongCodesAndLocksAfterFive() {
        OtpChallenge c = challenge();
        c.setSoLanSai(4);
        when(challenges.findForUpdate("id")).thenReturn(Optional.of(c));
        when(otpHasher.matches("123456", "hash")).thenReturn(false);
        assertEquals(SignupVerificationService.Status.ATTEMPTS_EXCEEDED, service.verify(request).status());
        assertEquals(5, c.getSoLanSai());
        verifyNoInteractions(accounts, sessions);
    }

    @Test
    void createsAccountAndClearsPendingDataAfterValidOtp() throws Exception {
        OtpChallenge c = challenge();
        c.setDuLieuDangKy(json.writeValueAsString(new SignupService.PendingSignup(
                "Host", "0905123456", "host@example.com", "079203001234", "bcrypt-hash")));
        when(challenges.findForUpdate("id")).thenReturn(Optional.of(c));
        when(otpHasher.matches("123456", "hash")).thenReturn(true);
        when(duplicates.findDuplicateFields(any())).thenReturn(List.of());
        when(accounts.saveAndFlush(any())).thenAnswer(invocation -> {
            TaiKhoan a = invocation.getArgument(0); a.setId(42); return a;
        });
        when(sessions.issue(any())).thenReturn(new SessionIssuer.TokenPair("access", "refresh", 1800));

        SignupVerificationService.Result result = service.verify(request);
        assertEquals(SignupVerificationService.Status.SUCCESS, result.status());
        assertEquals(42, result.response().userInfo().id());
        assertEquals(true, c.getDaDung());
        assertNull(c.getDuLieuDangKy());
    }
}