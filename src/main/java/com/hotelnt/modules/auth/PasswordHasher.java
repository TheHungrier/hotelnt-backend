package com.hotelnt.modules.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Bam va kiem tra mat khau bang bcrypt. */
@Component
class PasswordHasher {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    String hash(String password) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Missing password");
        return bcrypt.encode(password);
    }

    boolean matches(String password, String hash) {
        return password != null && hash != null && bcrypt.matches(password, hash);
    }
}
