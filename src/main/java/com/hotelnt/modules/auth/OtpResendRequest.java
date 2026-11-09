package com.hotelnt.modules.auth;

import jakarta.validation.constraints.NotBlank;

/** Du lieu yeu cau gui lai OTP cho mot phien xac thuc. */
public record OtpResendRequest(
        @NotBlank(message = "Mã phiên xác thực không được để trống")
        String challengeId
) {}
