package com.hotelnt.modules.auth;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class SigninVerifyController {
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
