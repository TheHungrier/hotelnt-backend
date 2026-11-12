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
import com.hotelnt.helper.SessionIssuer;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class RefreshController {
    private final RefreshService service;
    RefreshController(RefreshService service) { this.service = service; }

    @PostMapping("/refresh")
    SessionIssuer.TokenPair refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshService.Result result = service.refresh(request.refreshToken());
        return switch (result.status()) {
            case SUCCESS -> result.pair();
            case INVALID -> throw new AppException(ErrorCode.TOKEN_INVALID);
            case ACCOUNT_LOCKED -> throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        };
    }
}
