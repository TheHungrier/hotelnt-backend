package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpResendServiceTest {
    private final AuthChallengeRepository challenges = mock(AuthChallengeRepository.class);
    private final OtpCodeGenerator codes = mock(OtpCodeGenerator.class);
    private final OtpHasher hashes = mock(OtpHasher.class);
    private final OtpEmailSender email = mock(OtpEmailSender.class);
    private final OtpResendService service = new OtpResendService(challenges, codes, hashes, email, new ObjectMapper());

    @Test
    void replacesOtpAndRetainsSignupData() {
        OtpChallenge c = new OtpChallenge();
        c.setChallengeId("id");
        c.setEmail("host@example.com");
        c.setDuLieuDangKy("{\"hoTen\":\"Host\"}");
        c.setGuiLaiSauLuc(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusSeconds(1));
        when(email.isConfigured()).thenReturn(true);
        when(challenges.findForUpdate("id")).thenReturn(Optional.of(c));
        when(codes.generate()).thenReturn("001234");
        when(hashes.hash("001234")).thenReturn("new-hash");
        assertEquals(OtpResendService.Status.SUCCESS, service.resend(new OtpResendRequest("id")).status());
        assertEquals("new-hash", c.getOtpHash());
        assertTrue(c.getDuLieuDangKy().contains("Host"));
        assertTrue(c.getDuLieuDangKy().contains("\"resendCount\":1"));
        verify(email).send("host@example.com", "001234");
    }
}