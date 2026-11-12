package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpTimingTest {
    private final LocalDateTime sentAt = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void otpExpiresAtFiveMinutes() {
        LocalDateTime expiresAt = OtpTiming.expiresAt(sentAt);
        assertEquals(sentAt.plusSeconds(300), expiresAt);
        assertFalse(OtpTiming.isExpired(expiresAt, expiresAt.minusNanos(1)));
        assertTrue(OtpTiming.isExpired(expiresAt, expiresAt));
    }

    @Test
    void resendWaitRoundsUpAndEndsAtSixtySeconds() {
        LocalDateTime allowedAt = OtpTiming.resendAllowedAt(sentAt);
        assertEquals(60, OtpTiming.secondsUntilResend(allowedAt, sentAt));
        assertEquals(1, OtpTiming.secondsUntilResend(allowedAt, allowedAt.minusNanos(1)));
        assertEquals(0, OtpTiming.secondsUntilResend(allowedAt, allowedAt));
        assertEquals(0, OtpTiming.secondsUntilResend(allowedAt, allowedAt.plusSeconds(1)));
    }
}