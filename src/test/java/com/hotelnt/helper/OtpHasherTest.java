package com.hotelnt.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpHasherTest {
    private final OtpHasher hasher = new OtpHasher();

    @Test
    void verifiesCorrectOtpAndRejectsAnother() {
        String hash = hasher.hash("001234");

        assertTrue(hash.length() <= 255);
        assertNotEquals("001234", hash);
        assertTrue(hasher.matches("001234", hash));
        assertFalse(hasher.matches("001235", hash));
    }

    @Test
    void eachHashUsesFreshSalt() {
        assertNotEquals(hasher.hash("123456"), hasher.hash("123456"));
    }

    @Test
    void rejectsInvalidOtpAndMalformedHash() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("12345"));
        assertFalse(hasher.matches("12345", "invalid"));
        assertFalse(hasher.matches("123456", "pbkdf2-sha256-210000$invalid$invalid"));
        assertFalse(hasher.matches("123456", null));
    }
}