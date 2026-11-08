package com.hotelnt.modules.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank String resetToken,
        @NotBlank @Size(min = 8)
        @Pattern(regexp = "(?s)^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$") String newPassword) {}
