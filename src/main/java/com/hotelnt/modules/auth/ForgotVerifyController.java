package com.hotelnt.modules.auth;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class ForgotVerifyController {
    private final ForgotVerifyService service;
    ForgotVerifyController(ForgotVerifyService service) { this.service = service; }
    @PostMapping("/forgot-password/verify")
    Map<String, String> verify(@Valid @RequestBody ChallengeVerifyRequest request) {
        ForgotVerifyService.Result result = service.verify(request);
        ChallengeErrors.throwIfFailed(result.verification());
        return Map.of("resetToken", result.resetToken());
    }
}
