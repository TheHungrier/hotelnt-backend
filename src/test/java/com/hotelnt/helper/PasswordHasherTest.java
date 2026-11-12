package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
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