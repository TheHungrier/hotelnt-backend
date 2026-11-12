package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalFileStorageService implements StorageService {
    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục upload", e);
        }
    }

    @Override
    public String store(MultipartFile file, String type) {
        try {
            if (file.isEmpty()) throw new AppException(ErrorCode.VALIDATION_ERROR, "File rỗng");

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // Đổi tên file thành UUID để chống Path Traversal và trùng lặp
            String newFilename = UUID.randomUUID().toString() + extension;
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFilename)).normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Phát hiện truy cập đường dẫn không hợp lệ");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + newFilename;
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Lỗi khi lưu file vật lý.");
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;
        String filename = fileUrl.substring("/uploads/".length());
        try {
            Path fileToDelete = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
            if (fileToDelete.getParent().equals(this.rootLocation)) {
                Files.deleteIfExists(fileToDelete);
            }
        } catch (IOException e) {
            // Không throw exception ở đây để không chặn tiến trình xóa DB
        }
    }
}
