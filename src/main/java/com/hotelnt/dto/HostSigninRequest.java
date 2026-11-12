package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import jakarta.validation.constraints.NotBlank;

public record HostSigninRequest(@NotBlank(message = "Vui lòng nhập email hoặc số điện thoại") String loginIdentity,
                                @NotBlank(message = "Vui lòng nhập mật khẩu") String matKhau) {}
