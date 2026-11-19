package com.hotelnt.controller;

import com.hotelnt.dto.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.AdminIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ConfigController {

    private final ConfigService configService;
    private final AdminIdentityResolver identities;

    public ConfigController(ConfigService configService, AdminIdentityResolver identities) {
        this.configService = configService;
        this.identities = identities;
    }

    // ---- SEO Admin ----
    @GetMapping("/admin/configurations/seo")
    public ConfigDtos.AdminSeoConfigResponse getAdminSeoConfig(HttpServletRequest request) {
        return configService.getAdminSeoConfig(identities.resolve(request).role());
    }

    @PutMapping("/admin/configurations/seo")
    public ConfigDtos.SeoConfigUpdateResponse updateSeoConfig(
            HttpServletRequest request,
            @Valid @RequestBody ConfigDtos.SeoConfigUpdateRequest body) {
        return configService.updateSeoConfig(identities.resolve(request).role(), body);
    }

    @PostMapping("/admin/configurations/seo/og-image")
    public ConfigDtos.OgImageUploadResponse uploadOgImage(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        return configService.uploadOgImage(identities.resolve(request).role(), file);
    }

    // ---- AI Config Admin ----
    @GetMapping("/admin/configurations/ai")
    public ConfigDtos.AiConfigResponse getAiConfig(
            HttpServletRequest request) {
        return configService.getAiConfig(identities.resolve(request).role());
    }

    @PutMapping("/admin/configurations/ai")
    public ConfigDtos.AiConfigUpdateResponse updateAiConfig(
            HttpServletRequest request,
            @Valid @RequestBody ConfigDtos.AiConfigUpdateRequest body) {
        return configService.updateAiConfig(identities.resolve(request).role(), body);
    }
}
