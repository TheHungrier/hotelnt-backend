package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SignupServiceTest {
    private final SignupDuplicateChecker duplicates = mock(SignupDuplicateChecker.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final OtpCodeGenerator codes = mock(OtpCodeGenerator.class);
    private final OtpHasher otpHasher = mock(OtpHasher.class);
    private final OtpChallengeRepository challenges = mock(OtpChallengeRepository.class);
    private final OtpEmailSender email = mock(OtpEmailSender.class);
    private final SignupService service = new SignupService(duplicates, passwords, codes, otpHasher,
            challenges, email, new ObjectMapper());
    private final SignupRequest request = new SignupRequest(
            "Nguy?n Van A", "0905123456", "host01@example.com", "079203001234", "Password123!");

    @Test
    void createsChallengeWithHashedSecretsAndSendsOtp() {
        when(duplicates.findDuplicateFields(request)).thenReturn(List.of());
        when(codes.generate()).thenReturn("001234");
        when(passwords.hash(request.matKhau())).thenReturn("bcrypt-value");
        when(otpHasher.hash("001234")).thenReturn("otp-hash");

        SignupService.Response response = service.signup(request);
        ArgumentCaptor<OtpChallenge> saved = ArgumentCaptor.forClass(OtpChallenge.class);
        verify(challenges).save(saved.capture());
        verify(email).send(request.email(), "001234");
        assertEquals(saved.getValue().getChallengeId(), response.registrationId());
        assertEquals("otp-hash", saved.getValue().getOtpHash());
        assertFalse(saved.getValue().getDuLieuDangKy().contains("Password123!"));
        assertNotNull(saved.getValue().getHetHanLuc());
        assertEquals(300, response.otpExpiresIn());
    }

    @Test
    void rejectsDuplicateBeforeCreatingChallenge() {
        when(duplicates.findDuplicateFields(request)).thenReturn(List.of("email"));
        AppException ex = assertThrows(AppException.class, () -> service.signup(request));
        assertEquals(ErrorCode.ACCOUNT_DUPLICATED, ex.getCode());
        verifyNoInteractions(challenges, email, passwords, codes, otpHasher);
    }
}