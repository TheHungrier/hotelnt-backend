package com.hotelnt.modules.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank(message = "Không được để trống") String emailOrPhone) {}
