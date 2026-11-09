package com.hotelnt.modules.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class ForgotPasswordController {
    private final ForgotPasswordService service;
    ForgotPasswordController(ForgotPasswordService service) { this.service = service; }
    @PostMapping("/forgot-password")
    ForgotPasswordService.Response request(@Valid @RequestBody ForgotPasswordRequest request) { return service.request(request); }
}
