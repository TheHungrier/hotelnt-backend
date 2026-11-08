package com.hotelnt.modules.auth;

import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Tao ma OTP gom sau chu so, giu ca cac so 0 o dau. */
@Component
class OtpCodeGenerator {
    private final SecureRandom random;

    OtpCodeGenerator() {
        this(new SecureRandom());
    }

    OtpCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
}
