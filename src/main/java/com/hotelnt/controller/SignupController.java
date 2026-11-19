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
public class SignupController {
    private final SignupService service;

    SignupController(SignupService service) {
        this.service = service;
    }

    @PostMapping("/signup")
    SignupService.Response signup(@Valid @RequestBody SignupRequest request) {
        return service.signup(request);
    }

}
