package com.hotelnt.controller;

import com.hotelnt.dto.SeoDtos.SeoResponse;
import com.hotelnt.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API cong khai cho FE render the meta / Open Graph. Phan cap nhat cua Admin
 * thuoc module khac.
 */
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
