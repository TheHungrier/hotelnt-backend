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
class OtpResendController {
    private final OtpResendService service;
    OtpResendController(OtpResendService service) { this.service = service; }
    @PostMapping("/otp/resend")
    OtpResendService.Response resend(@Valid @RequestBody OtpResendRequest request) {
        OtpResendService.Result result = service.resend(request);
        return switch (result.status()) {
            case SUCCESS -> result.response();
            case EXPIRED -> throw new AppException(ErrorCode.OTP_EXPIRED);
            case TOO_SOON -> throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON, result.details());
            case LIMIT -> throw new AppException(ErrorCode.RATE_LIMITED);
        };
    }
}
