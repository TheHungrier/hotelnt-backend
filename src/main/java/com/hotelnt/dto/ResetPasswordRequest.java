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
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank String resetToken,
        @NotBlank @Size(min = 8)
        @Pattern(regexp = "(?s)^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s]).*$") String newPassword) {}
