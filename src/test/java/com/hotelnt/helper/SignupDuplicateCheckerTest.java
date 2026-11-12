package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignupDuplicateCheckerTest {
    private final SignupAccountRepository accounts = mock(SignupAccountRepository.class);
    private final SignupDuplicateChecker checker = new SignupDuplicateChecker(accounts);
    private final SignupRequest request = new SignupRequest(
            "Nguy?n Van A", "0905123456", "host01@example.com", "079203001234", "Password123!");

    @Test
    void returnsNoFieldsWhenAccountDoesNotExist() {
        assertEquals(List.of(), checker.findDuplicateFields(request));
    }

    @Test
    void returnsEveryDuplicatedField() {
        when(accounts.existsByEmail(request.email())).thenReturn(true);
        when(accounts.existsBySoDienThoai(request.soDienThoai())).thenReturn(true);
        when(accounts.existsByCccd(request.cccd())).thenReturn(true);

        assertEquals(List.of("email", "soDienThoai", "cccd"), checker.findDuplicateFields(request));
    }

    @Test
    void returnsOnlyTheDuplicatedField() {
        when(accounts.existsByCccd(request.cccd())).thenReturn(true);
        assertEquals(List.of("cccd"), checker.findDuplicateFields(request));
    }
}