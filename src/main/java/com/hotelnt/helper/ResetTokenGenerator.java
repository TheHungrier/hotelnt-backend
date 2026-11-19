package com.hotelnt.helper;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ResetTokenGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generate(String challengeId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return challengeId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String challengeId(String token) {
        if (token == null || token.length() != 80 || token.charAt(36) != '.')
            return null;
        String id = token.substring(0, 36);
        try {
            java.util.UUID.fromString(id);
            return id;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
