package com.hotelnt.modules.config;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.common.util.Times;
import com.hotelnt.domain.CauHinh;
import com.hotelnt.modules.media.StorageService;
import com.hotelnt.repository.CauHinhRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ConfigService {

    private static final List<String> SEO_KEYS = List.of(
            "seo.metaTitle", "seo.metaDescription", "seo.slugUrl", "seo.ogTitle", "seo.ogImageUrl"
    );

    private final CauHinhRepository repo;
    private final StorageService storageService;

    public ConfigService(CauHinhRepository repo, StorageService storageService) {
        this.repo = repo;
        this.storageService = storageService;
    }

    public ConfigDtos.SeoConfigResponse getSeoConfig() {
        Map<String, String> configs = repo.findAllById(SEO_KEYS).stream()
                .collect(Collectors.toMap(CauHinh::getKhoa, CauHinh::getGiaTri));

        return new ConfigDtos.SeoConfigResponse(
                configs.getOrDefault("seo.metaTitle", ""),
                configs.getOrDefault("seo.metaDescription", ""),
                configs.getOrDefault("seo.slugUrl", ""),
                configs.getOrDefault("seo.ogTitle", ""),
                configs.getOrDefault("seo.ogImageUrl", "")
        );
    }

    public ConfigDtos.AdminSeoConfigResponse getAdminSeoConfig(String role) {
        validateAdminRole(role);

        List<CauHinh> entities = repo.findAllById(SEO_KEYS);
        Map<String, String> configs = entities.stream()
                .collect(Collectors.toMap(CauHinh::getKhoa, CauHinh::getGiaTri));

        LocalDateTime latestUpdate = entities.stream()
                .map(CauHinh::getNgayCapNhat)
                .filter(t -> t != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return new ConfigDtos.AdminSeoConfigResponse(
                configs.getOrDefault("seo.metaTitle", ""),
                configs.getOrDefault("seo.metaDescription", ""),
                configs.getOrDefault("seo.slugUrl", ""),
                configs.getOrDefault("seo.ogTitle", ""),
                configs.getOrDefault("seo.ogImageUrl", ""),
                Times.toInstant(latestUpdate)
        );
    }

    @Transactional
    public ConfigDtos.SeoConfigUpdateResponse updateSeoConfig(String role, ConfigDtos.SeoConfigUpdateRequest req) {
        validateAdminRole(role);

        String newSlug = req.slugUrl().trim();

        // Kiểm tra xem slug có bị trùng lặp với cấu hình khác hay không (theo API Design: SLUG_DUPLICATED 409)
        boolean isDuplicated = repo.findAll().stream()
                .anyMatch(c -> !c.getKhoa().equals("seo.slugUrl") && newSlug.equalsIgnoreCase(c.getGiaTri()));
        if (isDuplicated) {
            throw new AppException(ErrorCode.SLUG_DUPLICATED);
        }

        LocalDateTime now = LocalDateTime.now();
        saveOrUpdateConfig("seo.metaTitle", req.metaTitle().trim(), now);
        saveOrUpdateConfig("seo.metaDescription", req.metaDescription().trim(), now);
        saveOrUpdateConfig("seo.slugUrl", newSlug, now);
        saveOrUpdateConfig("seo.ogTitle", req.ogTitle() != null ? req.ogTitle().trim() : "", now);
        saveOrUpdateConfig("seo.ogImageUrl", req.ogImageUrl() != null ? req.ogImageUrl().trim() : "", now);

        repo.flush();

        return new ConfigDtos.SeoConfigUpdateResponse("Cập nhật cấu hình SEO thành công", Times.toInstant(now));
    }

    public ConfigDtos.OgImageUploadResponse uploadOgImage(String role, MultipartFile file) {
        validateAdminRole(role);

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tệp ảnh không được để trống.");
        }

        // Tối đa 5MB
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String filename = file.getOriginalFilename();
        if (filename == null) filename = "";
        filename = filename.toLowerCase();

        if (!filename.endsWith(".jpg") && !filename.endsWith(".jpeg") && !filename.endsWith(".png") && !filename.endsWith(".webp")) {
            throw new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        // Kiểm tra tỷ lệ 1.91:1 theo tài liệu API (dung sai cho phép [1.80, 2.00])
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (height <= 0) {
                throw new AppException(ErrorCode.INVALID_IMAGE_RATIO);
            }
            double ratio = (double) width / height;
            if (ratio < 1.80 || ratio > 2.00) {
                throw new AppException(ErrorCode.INVALID_IMAGE_RATIO);
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể đọc nội dung ảnh để kiểm tra tỷ lệ.");
        }

        String url = storageService.store(file, "IMAGE");
        return new ConfigDtos.OgImageUploadResponse(url);
    }

    public ConfigDtos.AiConfigResponse getAiConfig(String role) {
        validateAdminRole(role);

        CauHinh entity = repo.findById("ai.tinNhanMacDinh").orElse(null);
        String tinNhan = entity != null ? entity.getGiaTri() : "Xin chào! Mình có thể giúp bạn tìm nơi lưu trú tại Nha Trang.";
        LocalDateTime updateTime = entity != null && entity.getNgayCapNhat() != null ? entity.getNgayCapNhat() : LocalDateTime.now();

        // Tuân thủ API Design & SRS FR-26: nguonDuLieu và phamViTraLoi là cố định, chỉ đọc
        return new ConfigDtos.AiConfigResponse(
                tinNhan,
                "APPROVED_ONLY",
                "LISTING_INFO_ONLY",
                Times.toInstant(updateTime)
        );
    }

    @Transactional
    public ConfigDtos.AiConfigUpdateResponse updateAiConfig(String role, ConfigDtos.AiConfigUpdateRequest req) {
        validateAdminRole(role);

        LocalDateTime now = LocalDateTime.now();
        saveOrUpdateConfig("ai.tinNhanMacDinh", req.tinNhanMacDinh().trim(), now);

        repo.flush();

        return new ConfigDtos.AiConfigUpdateResponse("Cập nhật cấu hình AI thành công", Times.toInstant(now));
    }

    private void saveOrUpdateConfig(String key, String value, LocalDateTime updateTime) {
        CauHinh config = repo.findById(key).orElseGet(() -> {
            CauHinh newConfig = new CauHinh();
            newConfig.setKhoa(key);
            return newConfig;
        });

        config.setGiaTri(value != null ? value : "");
        config.setNgayCapNhat(updateTime);
        repo.save(config);
    }

    private void validateAdminRole(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new AppException(ErrorCode.FORBIDDEN_ROLE);
        }
    }
}
