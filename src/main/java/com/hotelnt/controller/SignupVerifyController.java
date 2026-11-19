package com.hotelnt.controller;

import com.hotelnt.dto.*;
import com.hotelnt.service.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class SignupVerifyController {
    private final SignupVerificationService verification;

    SignupVerifyController(SignupVerificationService verification) {
        this.verification = verification;
    }

    @PostMapping("/signup/verify")
    SignupVerificationService.Response verify(@Valid @RequestBody SignupVerifyRequest request) {
        SignupVerificationService.Result result;
        try {
            result = verification.verify(request);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.ACCOUNT_DUPLICATED);
        }
        return switch (result.status()) {
            case SUCCESS -> result.response();
            case EXPIRED -> throw new AppException(ErrorCode.OTP_EXPIRED);
            case INVALID -> throw new AppException(ErrorCode.OTP_INVALID, result.details());
            case ATTEMPTS_EXCEEDED -> throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
            case DUPLICATE -> throw new AppException(ErrorCode.ACCOUNT_DUPLICATED, result.details());
        };
    }
}
