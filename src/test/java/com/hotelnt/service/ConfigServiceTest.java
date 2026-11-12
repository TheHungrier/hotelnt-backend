package com.hotelnt.service;

import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.repository.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private CauHinhRepository repo;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ConfigService configService;

    private CauHinh metaTitleConfig;
    private CauHinh slugConfig;
    private CauHinh aiMsgConfig;

    @BeforeEach
    void setUp() {
        metaTitleConfig = new CauHinh();
        metaTitleConfig.setKhoa("seo.metaTitle");
        metaTitleConfig.setGiaTri("Hotel Nha Trang");
        metaTitleConfig.setNgayCapNhat(LocalDateTime.now());

        slugConfig = new CauHinh();
        slugConfig.setKhoa("seo.slugUrl");
        slugConfig.setGiaTri("hotel-nha-trang");
        slugConfig.setNgayCapNhat(LocalDateTime.now());

        aiMsgConfig = new CauHinh();
        aiMsgConfig.setKhoa("ai.tinNhanMacDinh");
        aiMsgConfig.setGiaTri("Xin chào! Mình có thể giúp gì cho bạn?");
        aiMsgConfig.setNgayCapNhat(LocalDateTime.now());
    }

    // ==========================================
    // SEO Tests
    // ==========================================

    @Test
    void getSeoConfig_Success() {
        when(repo.findAllById(any())).thenReturn(List.of(metaTitleConfig, slugConfig));

        ConfigDtos.SeoConfigResponse res = configService.getSeoConfig();

        assertNotNull(res);
        assertEquals("Hotel Nha Trang", res.metaTitle());
        assertEquals("hotel-nha-trang", res.slugUrl());
    }

    @Test
    void getAdminSeoConfig_Success() {
        when(repo.findAllById(any())).thenReturn(List.of(metaTitleConfig, slugConfig));

        ConfigDtos.AdminSeoConfigResponse res = configService.getAdminSeoConfig("ADMIN");

        assertNotNull(res);
        assertEquals("Hotel Nha Trang", res.metaTitle());
        assertNotNull(res.ngayCapNhat());
    }

    @Test
    void getAdminSeoConfig_Fail_ForbiddenRole() {
        AppException ex = assertThrows(AppException.class, () -> configService.getAdminSeoConfig("USER"));
        assertEquals(ErrorCode.FORBIDDEN_ROLE, ex.getCode());
    }

    @Test
    void updateSeoConfig_Success() {
        ConfigDtos.SeoConfigUpdateRequest req = new ConfigDtos.SeoConfigUpdateRequest(
                "Tiêu đề mới", "Mô tả mới", "slug-moi", "OG Title", "/uploads/og.jpg"
        );

        when(repo.findAll()).thenReturn(List.of(slugConfig));
        when(repo.findByIdForUpdate(anyString())).thenReturn(Optional.of(new CauHinh()));

        ConfigDtos.SeoConfigUpdateResponse res = configService.updateSeoConfig("ADMIN", req);

        assertNotNull(res);
        assertTrue(res.message().contains("thành công"));
        verify(repo, atLeastOnce()).findByIdForUpdate("seo.slugUrl");
        verify(repo, atLeastOnce()).save(any(CauHinh.class));
    }

    @Test
    void updateSeoConfig_Fail_SlugDuplicated() {
        CauHinh otherConfig = new CauHinh();
        otherConfig.setKhoa("other.key");
        otherConfig.setGiaTri("slug-da-co");

        when(repo.findAll()).thenReturn(List.of(otherConfig));

        ConfigDtos.SeoConfigUpdateRequest req = new ConfigDtos.SeoConfigUpdateRequest(
                "Tiêu đề mới", "Mô tả mới", "slug-da-co", "OG Title", "/uploads/og.jpg"
        );

        AppException ex = assertThrows(AppException.class, () -> configService.updateSeoConfig("ADMIN", req));
        assertEquals(ErrorCode.SLUG_DUPLICATED, ex.getCode());
    }

    @Test
    void updateSeoConfig_Fail_ForbiddenRole() {
        ConfigDtos.SeoConfigUpdateRequest req = new ConfigDtos.SeoConfigUpdateRequest(
                "Tiêu đề mới", "Mô tả mới", "slug-moi", "OG Title", "/uploads/og.jpg"
        );

        AppException ex = assertThrows(AppException.class, () -> configService.updateSeoConfig("HOST", req));
        assertEquals(ErrorCode.FORBIDDEN_ROLE, ex.getCode());
    }

    // ==========================================
    // Open Graph Image Tests
    // ==========================================

    @Test
    void uploadOgImage_Success_ValidRatio() throws IOException {
        // T?o ?nh 1200x630 (t? l? chu?n ~1.9048)
        BufferedImage img = new BufferedImage(1200, 630, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        MockMultipartFile file = new MockMultipartFile("file", "og.jpg", "image/jpeg", baos.toByteArray());

        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/og-uuid.jpg");

        ConfigDtos.OgImageUploadResponse res = configService.uploadOgImage("ADMIN", file);

        assertNotNull(res);
        assertEquals("/uploads/og-uuid.jpg", res.url());
    }

    @Test
    void uploadOgImage_Fail_InvalidRatio() throws IOException {
        // Tạo ảnh vuông 500x500 (tỷ lệ 1:1, sai chuẩn 1.91:1)
        BufferedImage img = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        MockMultipartFile file = new MockMultipartFile("file", "og-square.png", "image/png", baos.toByteArray());

        AppException ex = assertThrows(AppException.class, () -> configService.uploadOgImage("ADMIN", file));
        assertEquals(ErrorCode.INVALID_IMAGE_RATIO, ex.getCode());
    }

    @Test
    void uploadOgImage_Fail_FileTooLarge() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);

        AppException ex = assertThrows(AppException.class, () -> configService.uploadOgImage("ADMIN", file));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getCode());
    }

    @Test
    void uploadOgImage_Fail_UnsupportedType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", new byte[]{1, 2, 3});

        AppException ex = assertThrows(AppException.class, () -> configService.uploadOgImage("ADMIN", file));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getCode());
    }

    @Test
    void uploadOgImage_Fail_ForbiddenRole() {
        MockMultipartFile file = new MockMultipartFile("file", "og.jpg", "image/jpeg", new byte[]{1, 2, 3});

        AppException ex = assertThrows(AppException.class, () -> configService.uploadOgImage("USER", file));
        assertEquals(ErrorCode.FORBIDDEN_ROLE, ex.getCode());
    }

    // ==========================================
    // AI Config Tests
    // ==========================================

    @Test
    void getAiConfig_Success() {
        when(repo.findById("ai.tinNhanMacDinh")).thenReturn(Optional.of(aiMsgConfig));

        ConfigDtos.AiConfigResponse res = configService.getAiConfig("ADMIN");

        assertNotNull(res);
        assertEquals("Xin chào! Mình có thể giúp gì cho bạn?", res.tinNhanMacDinh());
        assertEquals("APPROVED_ONLY", res.nguonDuLieu());
        assertEquals("LISTING_INFO_ONLY", res.phamViTraLoi());
        assertNotNull(res.ngayCapNhat());
    }

    @Test
    void getAiConfig_Fail_ForbiddenRole() {
        AppException ex = assertThrows(AppException.class, () -> configService.getAiConfig("USER"));
        assertEquals(ErrorCode.FORBIDDEN_ROLE, ex.getCode());
    }

    @Test
    void updateAiConfig_Success() {
        ConfigDtos.AiConfigUpdateRequest req = new ConfigDtos.AiConfigUpdateRequest("Tin nhắn hỗ trợ mới.");
        when(repo.findByIdForUpdate("ai.tinNhanMacDinh")).thenReturn(Optional.of(aiMsgConfig));

        ConfigDtos.AiConfigUpdateResponse res = configService.updateAiConfig("ADMIN", req);

        verify(repo).findByIdForUpdate("ai.tinNhanMacDinh");
        assertNotNull(res);
        assertTrue(res.message().contains("thành công"));
        verify(repo, times(1)).save(argThat(c ->
                c.getKhoa().equals("ai.tinNhanMacDinh") && c.getGiaTri().equals("Tin nhắn hỗ trợ mới.")
        ));
    }

    @Test
    void updateAiConfig_Fail_ForbiddenRole() {
        ConfigDtos.AiConfigUpdateRequest req = new ConfigDtos.AiConfigUpdateRequest("Tin nhắn hỗ trợ mới.");

        AppException ex = assertThrows(AppException.class, () -> configService.updateAiConfig("HOST", req));
        assertEquals(ErrorCode.FORBIDDEN_ROLE, ex.getCode());
    }

    @Test
    void uploadOgImage_WebpSupport_ImageReaderAvailable() {
        // Kiểm tra thư viện TwelveMonkeys WebP ImageReader đã đăng ký thành công trong ImageIO SPI
        assertTrue(ImageIO.getImageReadersByFormatName("webp").hasNext(), "TwelveMonkeys WebP ImageReader phải có sẵn trong ImageIO");
    }

    @Test
    void uploadOgImage_WebpSupport_DecodeAndValidateRatio() {
        // Byte array của ảnh WebP hợp lệ 1x1 (lossless WebP chuẩn)
        byte[] minimal1x1Webp = new byte[]{
            0x52, 0x49, 0x46, 0x46, 0x1A, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50,
            0x56, 0x50, 0x38, 0x4C, 0x0E, 0x00, 0x00, 0x00, 0x2F, 0x00, 0x00, 0x00,
            0x10, 0x07, 0x10, 0x11, 0x11, (byte) 0x88, (byte) 0x88, (byte) 0xFE, 0x07, 0x00
        };

        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", minimal1x1Webp);

        // Khi đọc được ảnh WebP (1x1), tỷ lệ 1:1 sẽ bị từ chối với INVALID_IMAGE_RATIO (chứ không phải UNSUPPORTED_MEDIA_TYPE)
        AppException ex = assertThrows(AppException.class, () -> configService.uploadOgImage("ADMIN", file));
        assertEquals(ErrorCode.INVALID_IMAGE_RATIO, ex.getCode());
    }
}
