package com.hotelnt.controller;

import com.hotelnt.common.error.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.ChallengeErrors;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class ForgotVerifyController {
    private final ForgotVerifyService service;
    ForgotVerifyController(ForgotVerifyService service) { this.service = service; }
    @PostMapping("/forgot-password/verify")
    Map<String, String> verify(@Valid @RequestBody ChallengeVerifyRequest request) {
        ForgotVerifyService.Result result = service.verify(request);
        ChallengeErrors.throwIfFailed(result.verification());
        return Map.of("resetToken", result.resetToken());
    }
}
