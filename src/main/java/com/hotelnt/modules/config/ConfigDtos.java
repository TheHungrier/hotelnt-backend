package com.hotelnt.modules.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ConfigDtos {
    private ConfigDtos() {}

    // ---- SEO Public ----
    public record SeoConfigResponse(
            String metaTitle,
            String metaDescription,
            String slugUrl,
            String ogTitle,
            String ogImageUrl
    ) {}

    // ---- SEO Admin ----
    public record AdminSeoConfigResponse(
            String metaTitle,
            String metaDescription,
            String slugUrl,
            String ogTitle,
            String ogImageUrl,
            Instant ngayCapNhat
    ) {}

    public record SeoConfigUpdateRequest(
            @NotBlank(message = "metaTitle không được để trống")
            @Size(max = 60, message = "metaTitle tối đa 60 ký tự")
            String metaTitle,

            @NotBlank(message = "metaDescription không được để trống")
            @Size(max = 160, message = "metaDescription tối đa 160 ký tự")
            String metaDescription,

            @NotBlank(message = "slugUrl không được để trống")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "slugUrl chỉ chứa chữ thường, số và dấu gạch ngang (-)")
            String slugUrl,

            @Size(max = 60, message = "ogTitle tối đa 60 ký tự")
            String ogTitle,

            String ogImageUrl
    ) {}

    public record SeoConfigUpdateResponse(
            String message,
            Instant ngayCapNhat
    ) {}

    public record OgImageUploadResponse(
            String url
    ) {}

    // ---- AI Config Admin ----
    public record AiConfigResponse(
            String tinNhanMacDinh,
            String nguonDuLieu,
            String phamViTraLoi,
            Instant ngayCapNhat
    ) {}

    public record AiConfigUpdateRequest(
            @NotBlank(message = "tinNhanMacDinh không được để trống")
            @Size(min = 1, max = 200, message = "tinNhanMacDinh từ 1 đến 200 ký tự")
            String tinNhanMacDinh
    ) {}

    public record AiConfigUpdateResponse(
            String message,
            Instant ngayCapNhat
    ) {}
}
