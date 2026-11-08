package com.hotelnt.modules.auth;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class ResetPasswordController {
    private final ResetPasswordService service;
    ResetPasswordController(ResetPasswordService service) { this.service = service; }
    @PostMapping("/reset-password")
    Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest request) {
        if (!service.reset(request)) throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        return Map.of("message", "Đặt lại mật khẩu thành công");
    }
}
