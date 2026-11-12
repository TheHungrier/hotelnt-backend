package com.hotelnt.helper;

import java.time.Duration;
import java.time.LocalDateTime;

/** Tinh thoi han OTP va thoi gian cho gui lai theo hop dong API. */
final public class OtpTiming {
    public static final int EXPIRES_IN_SECONDS = 300;
    public static final int RESEND_AFTER_SECONDS = 60;

    private OtpTiming() {
    }

    public static LocalDateTime expiresAt(LocalDateTime sentAt) {
        return sentAt.plusSeconds(EXPIRES_IN_SECONDS);
    }

    public static LocalDateTime resendAllowedAt(LocalDateTime sentAt) {
        return sentAt.plusSeconds(RESEND_AFTER_SECONDS);
    }

    public static boolean isExpired(LocalDateTime expiresAt, LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public static long secondsUntilResend(LocalDateTime resendAllowedAt, LocalDateTime now) {
        if (!now.isBefore(resendAllowedAt))
            return 0;
        Duration remaining = Duration.between(now, resendAllowedAt);
        return remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1);
    }
}
