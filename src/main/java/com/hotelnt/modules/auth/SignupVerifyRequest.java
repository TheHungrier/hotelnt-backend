package com.hotelnt.modules.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Du lieu xac thuc OTP khi dang ky tai khoan Host. */
public record SignupVerifyRequest(
        @NotBlank(message = "Mã đăng ký không được để trống")
        String registrationId,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải gồm đúng 6 chữ số")
        String otpCode
) {}
