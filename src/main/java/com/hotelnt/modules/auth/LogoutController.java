package com.hotelnt.modules.auth;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class LogoutController {
    private final LogoutService service;
    LogoutController(LogoutService service) { this.service = service; }

    @PostMapping("/logout")
    Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @Valid @RequestBody RefreshTokenRequest request) {
        if (!service.logout(authorization, request.refreshToken())) throw new AppException(ErrorCode.TOKEN_INVALID);
        return Map.of("message", "Đăng xuất thành công");
    }
}
