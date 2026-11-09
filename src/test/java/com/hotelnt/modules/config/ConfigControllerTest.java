package com.hotelnt.modules.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.common.error.GlobalExceptionHandler;
import com.hotelnt.modules.admin.AdminIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigController.class)
@Import(GlobalExceptionHandler.class)
class ConfigControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigService configService;

    @MockBean
    private AdminIdentityResolver identities;

    @BeforeEach
    void authenticateAsAdmin() {
        when(identities.resolve(any(HttpServletRequest.class)))
                .thenReturn(new AdminIdentityResolver.Identity(1, "admin@example.com", "ADMIN"));
    }

    // ==========================================
    // SEO Admin Endpoints Tests
    // ==========================================

    @Test
    void getAdminSeoConfig_Success() throws Exception {
        when(configService.getAdminSeoConfig("ADMIN"))
                .thenReturn(new ConfigDtos.AdminSeoConfigResponse(
                        "Hotel Nha Trang", "Tìm nơi lưu trú", "hotel-nha-trang", "OG Title", "/uploads/og.jpg", Instant.now()
                ));

        mvc.perform(get("/admin/configurations/seo")
                        .header("X-Mock-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metaTitle").value("Hotel Nha Trang"));
    }

    @Test
    void getAdminSeoConfig_Fail_Forbidden() throws Exception {
        when(identities.resolve(any(HttpServletRequest.class)))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN_ROLE));

        mvc.perform(get("/admin/configurations/seo")
                        .header("X-Mock-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void updateSeoConfig_Success() throws Exception {
        ConfigDtos.SeoConfigUpdateRequest req = new ConfigDtos.SeoConfigUpdateRequest(
                "Tiêu đề mới", "Mô tả mới", "slug-moi", "OG Title", "/uploads/og.jpg"
        );

        when(configService.updateSeoConfig(eq("ADMIN"), any()))
                .thenReturn(new ConfigDtos.SeoConfigUpdateResponse("Cập nhật cấu hình SEO thành công", Instant.now()));

        mvc.perform(put("/admin/configurations/seo")
                        .header("X-Mock-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật cấu hình SEO thành công"));
    }

    @Test
    void updateSeoConfig_Fail_ValidationError_InvalidSlug() throws Exception {
        // Slug chứa ký tự hoa và khoảng trắng -> vi phạm regex ^[a-z0-9-]+$
        ConfigDtos.SeoConfigUpdateRequest req = new ConfigDtos.SeoConfigUpdateRequest(
                "Tiêu đề", "Mô tả", "Slug Không Hợp Lệ!", "OG Title", "/uploads/og.jpg"
        );

        mvc.perform(put("/admin/configurations/seo")
                        .header("X-Mock-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void uploadOgImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "og.jpg", "image/jpeg", "image-bytes".getBytes());

        when(configService.uploadOgImage(eq("ADMIN"), any()))
                .thenReturn(new ConfigDtos.OgImageUploadResponse("/uploads/og-uuid.jpg"));

        mvc.perform(multipart("/admin/configurations/seo/og-image")
                        .file(file)
                        .header("X-Mock-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/og-uuid.jpg"));
    }

    // ==========================================
    // AI Config Admin Endpoints Tests
    // ==========================================

    @Test
    void getAiConfig_Success() throws Exception {
        when(configService.getAiConfig("ADMIN"))
                .thenReturn(new ConfigDtos.AiConfigResponse(
                        "Xin chào!", "APPROVED_ONLY", "LISTING_INFO_ONLY", Instant.now()
                ));

        mvc.perform(get("/admin/configurations/ai")
                        .header("X-Mock-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tinNhanMacDinh").value("Xin chào!"))
                .andExpect(jsonPath("$.nguonDuLieu").value("APPROVED_ONLY"))
                .andExpect(jsonPath("$.phamViTraLoi").value("LISTING_INFO_ONLY"));
    }

    @Test
    void updateAiConfig_Success() throws Exception {
        ConfigDtos.AiConfigUpdateRequest req = new ConfigDtos.AiConfigUpdateRequest("Tin nhắn mới");

        when(configService.updateAiConfig(eq("ADMIN"), any()))
                .thenReturn(new ConfigDtos.AiConfigUpdateResponse("Cập nhật cấu hình AI thành công", Instant.now()));

        mvc.perform(put("/admin/configurations/ai")
                        .header("X-Mock-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật cấu hình AI thành công"));
    }

    @Test
    void updateAiConfig_Fail_ValidationError_TooLong() throws Exception {
        String longText = "a".repeat(201); // 201 ký tự > max 200
        ConfigDtos.AiConfigUpdateRequest req = new ConfigDtos.AiConfigUpdateRequest(longText);

        mvc.perform(put("/admin/configurations/ai")
                        .header("X-Mock-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
