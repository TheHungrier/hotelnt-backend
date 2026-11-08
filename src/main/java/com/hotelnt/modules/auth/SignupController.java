package com.hotelnt.modules.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class SignupController {
    private final SignupService service;

    SignupController(SignupService service) {
        this.service = service;
    }

    @PostMapping("/signup")
    SignupService.Response signup(@Valid @RequestBody SignupRequest request) {
        return service.signup(request);
    }

}
