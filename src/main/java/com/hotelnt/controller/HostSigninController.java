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
public class HostSigninController {
    private final HostSigninService service;
    HostSigninController(HostSigninService service) { this.service = service; }
    @PostMapping("/signin")
    HostSigninService.Response signin(@Valid @RequestBody HostSigninRequest request) {
        HostSigninService.Result result = service.signin(request);
        SigninErrors.throwIfFailed(result.status(), result.details());
        return result.response();
    }
}
