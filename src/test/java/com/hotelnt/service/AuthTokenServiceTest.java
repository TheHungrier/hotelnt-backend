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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthTokenServiceTest {
    private final AuthTokenService tokens = new AuthTokenService("01234567890123456789012345678901", 604800);

    @Test
    void signsAndVerifiesTokensAndRejectsWrongType() {
        String access = tokens.accessToken(5, "session-id");
        assertEquals("5", tokens.verify(access, "access").getSubject());
        assertEquals("session-id", tokens.verify(access, "access").get("sid", String.class));
        assertThrows(IllegalArgumentException.class, () -> tokens.verify(access, "refresh"));
    }

    @Test
    void refreshTokensAreUniqueAndInvalidSignaturesAreRejected() {
        String first = tokens.refreshToken(5, "session-id");
        assertNotEquals(first, tokens.refreshToken(5, "session-id"));
        AuthTokenService other = new AuthTokenService("abcdefghijklmnopqrstuvwxyz123456", 604800);
        assertThrows(IllegalArgumentException.class, () -> other.verify(first, "refresh"));
    }

    @Test
    void requiresARealSigningKey() {
        AuthTokenService missingKey = new AuthTokenService("", 604800);
        assertThrows(IllegalStateException.class, () -> missingKey.accessToken(5, "session-id"));
    }
}