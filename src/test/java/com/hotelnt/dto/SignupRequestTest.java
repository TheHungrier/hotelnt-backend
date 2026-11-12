package com.hotelnt.dto;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignupRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidSignup() {
        assertTrue(validator.validate(new SignupRequest(
                "Nguy?n Van A", "0905123456", "host01@example.com", "079203001234", "Password123!"))
                .isEmpty());
    }

    @Test
    void rejectsInvalidFields() {
        SignupRequest request = new SignupRequest(" ", "1905123456", "invalid-email", "123", "password");
        Set<String> fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertEquals(Set.of("hoTen", "soDienThoai", "email", "cccd", "matKhau"), fields);
    }

    @Test
    void rejectsPasswordMissingSpecialCharacter() {
        SignupRequest request = new SignupRequest(
                "Nguy?n Van A", "0905123456", "host01@example.com", "079203001234", "Password123");
        assertEquals(Set.of("matKhau"), validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet()));
    }
}