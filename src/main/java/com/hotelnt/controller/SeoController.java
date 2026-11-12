package com.hotelnt.controller;

import com.hotelnt.common.error.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.SeoDtos.SeoResponse;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API cong khai cho FE render the meta / Open Graph. Phan cap nhat cua Admin thuoc module khac. */
@RestController
@RequestMapping("/configurations/seo")
public class SeoController {

    private final SeoService service;

    public SeoController(SeoService service) {
        this.service = service;
    }

    // GET /api/configurations/seo
    @GetMapping
    public SeoResponse get() {
        return service.get();
    }
}
