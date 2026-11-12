package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
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
