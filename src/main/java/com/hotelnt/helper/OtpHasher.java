package com.hotelnt.helper;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

/** Bam OTP co salt truoc khi luu vao OtpChallenge.otpHash. */
@Component
public class OtpHasher {
    private static final String FORMAT = "pbkdf2-sha256-210000";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    public String hash(String otp) {
        if (!isOtp(otp))
            throw new IllegalArgumentException("OTP must contain six digits");
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(otp, salt);
        return FORMAT + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String otp, String storedHash) {
        if (!isOtp(otp) || storedHash == null || storedHash.length() > 255)
            return false;
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 3 || !FORMAT.equals(parts[0]))
            return false;
        try {
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            if (salt.length != SALT_BYTES || expected.length != HASH_BYTES)
                return false;
            return MessageDigest.isEqual(expected, derive(otp, salt));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isOtp(String otp) {
        return otp != null && otp.matches("[0-9]{6}");
    }

    private static byte[] derive(String otp, byte[] salt) {
        char[] chars = otp.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot hash OTP", ex);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}
