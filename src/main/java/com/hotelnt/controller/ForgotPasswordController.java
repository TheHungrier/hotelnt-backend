package com.hotelnt.controller;

import com.hotelnt.dto.*;
import com.hotelnt.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class ForgotPasswordController {
    private final ForgotPasswordService service;
    ForgotPasswordController(ForgotPasswordService service) { this.service = service; }
    @PostMapping("/forgot-password")
    ForgotPasswordService.Response request(@Valid @RequestBody ForgotPasswordRequest request) { return service.request(request); }
}
