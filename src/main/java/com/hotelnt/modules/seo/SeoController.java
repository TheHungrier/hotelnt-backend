package com.hotelnt.modules.seo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API cong khai cho FE render the meta / Open Graph. Phan cap nhat cua Admin thuoc module khac. */
@RestController
@RequestMapping("/configurations/seo")
public class SeoController {

    public record SeoResponse(String metaTitle, String metaDescription, String slugUrl,
                              String ogTitle, String ogImageUrl) {}

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
