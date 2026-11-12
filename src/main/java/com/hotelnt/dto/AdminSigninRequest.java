package com.hotelnt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminSigninRequest(@NotBlank @Email String email, @NotBlank String matKhau) {}
