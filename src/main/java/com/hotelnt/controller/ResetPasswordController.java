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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class ResetPasswordController {
    private final ResetPasswordService service;
    ResetPasswordController(ResetPasswordService service) { this.service = service; }
    @PostMapping("/reset-password")
    Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest request) {
        if (!service.reset(request)) throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        return Map.of("message", "Đặt lại mật khẩu thành công");
    }
}
