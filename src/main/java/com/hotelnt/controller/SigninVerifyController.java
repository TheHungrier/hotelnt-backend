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
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class SigninVerifyController {
    private final SigninVerifyService service;
    SigninVerifyController(SigninVerifyService service) { this.service = service; }
    @PostMapping("/signin/verify")
    SignupVerificationService.Response verify(@Valid @RequestBody ChallengeVerifyRequest request) {
        SigninVerifyService.Result result = service.verify(request);
        ChallengeErrors.throwIfFailed(result.verification());
        if (result.accountLocked()) throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        return result.response();
    }
}
