package com.hotelnt.modules.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verifiesPasswordAndRejectsDifferentPassword() {
        String hash = hasher.hash("Password123!");
        assertNotEquals("Password123!", hash);
        assertTrue(hasher.matches("Password123!", hash));
        assertFalse(hasher.matches("Different123!", hash));
    }

    @Test
    void rejectsMissingPassword() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertFalse(hasher.matches(null, "hash"));
    }
}
