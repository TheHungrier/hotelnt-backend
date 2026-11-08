package com.hotelnt.modules.auth;

import jakarta.validation.constraints.NotBlank;

public record HostSigninRequest(@NotBlank(message = "Vui lòng nhập email hoặc số điện thoại") String loginIdentity,
                                @NotBlank(message = "Vui lòng nhập mật khẩu") String matKhau) {}
