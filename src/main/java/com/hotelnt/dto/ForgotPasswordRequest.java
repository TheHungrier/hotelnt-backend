package com.hotelnt.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank(message = "Không được để trống") String emailOrPhone) {
}
