package com.hotelnt.modules.config;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    // ---- SEO Public ----
    @GetMapping("/configurations/seo")
    public ConfigDtos.SeoConfigResponse getSeoConfig() {
        return configService.getSeoConfig();
    }

    // ---- SEO Admin ----
    @GetMapping("/admin/configurations/seo")
    public ConfigDtos.AdminSeoConfigResponse getAdminSeoConfig(
            // TODO: Bỏ header mock X-Mock-Role khi module Auth tích hợp Spring Security
            @RequestHeader(value = "X-Mock-Role", defaultValue = "USER") String role) {
        return configService.getAdminSeoConfig(role);
    }

    @PutMapping("/admin/configurations/seo")
    public ConfigDtos.SeoConfigUpdateResponse updateSeoConfig(
            @RequestHeader(value = "X-Mock-Role", defaultValue = "USER") String role,
            @Valid @RequestBody ConfigDtos.SeoConfigUpdateRequest request) {
        return configService.updateSeoConfig(role, request);
    }

    @PostMapping("/admin/configurations/seo/og-image")
    public ConfigDtos.OgImageUploadResponse uploadOgImage(
            @RequestHeader(value = "X-Mock-Role", defaultValue = "USER") String role,
            @RequestParam("file") MultipartFile file) {
        return configService.uploadOgImage(role, file);
    }

    // ---- AI Config Admin ----
    @GetMapping("/admin/configurations/ai")
    public ConfigDtos.AiConfigResponse getAiConfig(
            @RequestHeader(value = "X-Mock-Role", defaultValue = "USER") String role) {
        return configService.getAiConfig(role);
    }

    @PutMapping("/admin/configurations/ai")
    public ConfigDtos.AiConfigUpdateResponse updateAiConfig(
            @RequestHeader(value = "X-Mock-Role", defaultValue = "USER") String role,
            @Valid @RequestBody ConfigDtos.AiConfigUpdateRequest request) {
        return configService.updateAiConfig(role, request);
    }
}
