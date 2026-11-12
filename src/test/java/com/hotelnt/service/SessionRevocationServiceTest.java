package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionRevocationServiceTest {
    private final SessionRevocationRepository repository = mock(SessionRevocationRepository.class);
    private final SessionRevocationService service = new SessionRevocationService(repository);

    @Test
    void revokesOnlyTheSpecifiedSession() {
        when(repository.revokeSession("session-id")).thenReturn(1);
        assertEquals(1, service.revokeSession("session-id"));
    }

    @Test
    void revokesAllSessionsForTheSpecifiedAccount() {
        when(repository.revokeAccountSessions(42)).thenReturn(2);
        assertEquals(2, service.revokeAccountSessions(42));
    }

    @Test
    void rejectsMissingIdentifiersWithoutUpdatingDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.revokeSession(" "));
        assertThrows(IllegalArgumentException.class, () -> service.revokeAccountSessions(0));
        verifyNoInteractions(repository);
    }
}