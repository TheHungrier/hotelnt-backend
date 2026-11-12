package com.hotelnt.controller;

import com.hotelnt.common.error.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.HostIdentityResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/host")
public class HostController {
    private final HostService service;
    private final HostIdentityResolver identities;
    private final ObjectMapper mapper;
    private final Validator validator;
    public HostController(HostService service,HostIdentityResolver identities,ObjectMapper mapper,Validator validator) {
        this.service=service;this.identities=identities;this.mapper=mapper;this.validator=validator;
    }
    @GetMapping("/profile") public Profile profile(HttpServletRequest req) {return service.profile(identities.resolve(req));}
    @PatchMapping("/profile") public ProfileUpdated updateProfile(HttpServletRequest req,@RequestBody JsonNode body) {
        var identity=identities.resolve(req);
        if(body.has("email") || body.has("cccd"))throw new AppException(ErrorCode.FIELD_NOT_EDITABLE);
        if(!body.isObject())throw new AppException(ErrorCode.VALIDATION_ERROR);
        body.fieldNames().forEachRemaining(k->{if(!Set.of("hoTen","soDienThoai").contains(k))throw new AppException(ErrorCode.VALIDATION_ERROR);});
        ProfileUpdate q;
        try {q=mapper.treeToValue(body,ProfileUpdate.class);}catch(Exception e){throw new AppException(ErrorCode.VALIDATION_ERROR);}
        var errors=validator.validate(q);
        if(!errors.isEmpty())throw new AppException(ErrorCode.VALIDATION_ERROR,errors.stream().map(v->Map.of("field",v.getPropertyPath().toString(),"message",v.getMessage())).toList());
        return service.updateProfile(identity,q);
    }
    @PostMapping("/profile/change-password") public Message changePassword(HttpServletRequest req,@Valid @RequestBody PasswordChange q) {return service.changePassword(identities.resolve(req),q);}
    @GetMapping("/listings") public ListingPage list(HttpServletRequest req,@Valid ListingQuery q) {return service.list(identities.resolve(req),q);}
    @GetMapping("/listings/{maCSLT}") public ListingDetail detail(HttpServletRequest req,@PathVariable int maCSLT) {return service.detail(identities.resolve(req),maCSLT);}
    @PostMapping("/listings") @ResponseStatus(HttpStatus.CREATED)
    public Created create(HttpServletRequest req,@Valid @RequestBody ListingWrite q) {return service.create(identities.resolve(req),q);}
    @PutMapping("/listings/{maCSLT}") public Updated update(HttpServletRequest req,@PathVariable int maCSLT,@Valid @RequestBody ListingWrite q) {return service.update(identities.resolve(req),maCSLT,q);}
    @DeleteMapping("/listings/{maCSLT}") public Message delete(HttpServletRequest req,@PathVariable int maCSLT) {return service.delete(identities.resolve(req),maCSLT);}
    @PostMapping("/listings/{maCSLT}/submit-review") public Submitted submit(HttpServletRequest req,@PathVariable int maCSLT) {return service.submit(identities.resolve(req),maCSLT);}
    @PostMapping("/listings/{maCSLT}/withdraw") public Withdrawn withdraw(HttpServletRequest req,@PathVariable int maCSLT) {return service.withdraw(identities.resolve(req),maCSLT);}
}
