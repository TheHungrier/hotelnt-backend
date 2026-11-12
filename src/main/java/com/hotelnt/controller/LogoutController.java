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
public class LogoutController {
    private final LogoutService service;
    LogoutController(LogoutService service) { this.service = service; }

    @PostMapping("/logout")
    Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @Valid @RequestBody RefreshTokenRequest request) {
        if (!service.logout(authorization, request.refreshToken())) throw new AppException(ErrorCode.TOKEN_INVALID);
        return Map.of("message", "Đăng xuất thành công");
    }
}
