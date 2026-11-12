package com.hotelnt.controller;

import com.hotelnt.dto.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.SigninErrors;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AdminSigninController {
    private final AdminSigninService service;
    AdminSigninController(AdminSigninService service) { this.service = service; }
    @PostMapping("/admin/signin")
    AdminSigninService.Response signin(@Valid @RequestBody AdminSigninRequest request) {
        AdminSigninService.Result result = service.signin(request);
        SigninErrors.throwIfFailed(result.status(), result.details());
        return result.response();
    }
}
