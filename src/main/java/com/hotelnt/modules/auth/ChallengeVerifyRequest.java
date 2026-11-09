package com.hotelnt.modules.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChallengeVerifyRequest(@NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otpCode) {}
