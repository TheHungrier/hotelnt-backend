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

public record ChallengeVerifyRequest(@NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otpCode) {}
