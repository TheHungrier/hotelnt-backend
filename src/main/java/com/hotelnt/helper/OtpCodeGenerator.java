package com.hotelnt.helper;

import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Tao ma OTP gom sau chu so, giu ca cac so 0 o dau. */
@Component
public class OtpCodeGenerator {
    private final SecureRandom random;

    public OtpCodeGenerator() {
        this(new SecureRandom());
    }

    public OtpCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
}
