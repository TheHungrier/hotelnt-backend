package com.hotelnt.modules.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidSignupVerificationAndResend() {
        assertTrue(validator.validate(new SignupVerifyRequest("registration-id", "001234")).isEmpty());
        assertTrue(validator.validate(new OtpResendRequest("registration-id")).isEmpty());
    }

    @Test
    void rejectsMissingIdAndInvalidOtp() {
        Set<String> fields = validator.validate(new SignupVerifyRequest(" ", "12345a")).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertEquals(Set.of("registrationId", "otpCode"), fields);
    }

    @Test
    void rejectsMissingResendChallengeId() {
        assertEquals(1, validator.validate(new OtpResendRequest(null)).size());
    }
}
