package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import jakarta.validation.constraints.NotBlank;

/** Du lieu yeu cau gui lai OTP cho mot phien xac thuc. */
public record OtpResendRequest(
        @NotBlank(message = "Mã phiên xác thực không được để trống")
        String challengeId
) {}
