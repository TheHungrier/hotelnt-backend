package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MediaService {
    public static final int MIN_IMAGES = 3;
    public static final int MAX_IMAGES = 100;
    public static final int MAX_VIDEOS = 5;
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L; // 10MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024L; // 100MB

    private final CoSoLuuTruRepository coSoRepo;
    private final MediaRepository mediaRepo;
    private final StorageService storageService;

    public MediaService(CoSoLuuTruRepository coSoRepo, MediaRepository mediaRepo, StorageService storageService) {
        this.coSoRepo = coSoRepo;
        this.mediaRepo = mediaRepo;
        this.storageService = storageService;
    }

    public MediaDtos.MediaUploadResponse upload(int maCSLT, int hostId, MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tệp tải lên không được để trống.");
        }

        if (type == null || (!"IMAGE".equalsIgnoreCase(type) && !"VIDEO".equalsIgnoreCase(type))) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Loại media phải là IMAGE hoặc VIDEO.");
        }
        String normType = type.toUpperCase();
        boolean isImage = "IMAGE".equals(normType);

        long maxSize = isImage ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        validateFileType(file, normType);

        String url = storageService.store(file, normType);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        try {
                            storageService.delete(url);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }

        try {
            CoSoLuuTru coSo = coSoRepo.findByIdForUpdate(maCSLT)
                    .orElseThrow(() -> new AppException(ErrorCode.LISTING_NOT_FOUND));

            if (coSo.getHost() == null || !coSo.getHost().getId().equals(hostId)) {
                throw new AppException(ErrorCode.NOT_OWNER);
            }
            if (ListingStatus.PENDING.equals(coSo.getTrangThai())) {
                throw new AppException(ErrorCode.LISTING_LOCKED_PENDING);
            }
            if (ListingStatus.REMOVED.equals(coSo.getTrangThai())) {
                throw new AppException(ErrorCode.LISTING_REMOVED);
            }

            List<Media> currentMedia = coSo.getMedia() != null ? coSo.getMedia() : List.of();
            long imageCount = currentMedia.stream().filter(m -> "IMAGE".equalsIgnoreCase(m.getLoai())).count();
            long videoCount = currentMedia.stream().filter(m -> "VIDEO".equalsIgnoreCase(m.getLoai())).count();
            if (isImage && imageCount >= MAX_IMAGES) {
                throw new AppException(ErrorCode.MEDIA_LIMIT_EXCEEDED);
            }
            if (!isImage && videoCount >= MAX_VIDEOS) {
                throw new AppException(ErrorCode.MEDIA_LIMIT_EXCEEDED);
            }

            Media media = new Media();
            media.setDuongDan(url);
            media.setKichThuoc((int) file.getSize());
            media.setLoai(normType);
            media.setMimeType(file.getContentType() != null ? file.getContentType() : (isImage ? "image/jpeg" : "video/mp4"));
            int nextThuTu = currentMedia.stream()
                    .mapToInt(m -> m.getThuTu() != null ? m.getThuTu() : 0)
                    .max()
                    .orElse(0) + 1;
            media.setThuTu(nextThuTu);
            media.setCoSo(coSo);

            Media savedMedia = mediaRepo.save(media);

            // Quy tắc nghiệp vụ từ API Design: CSLT đã duyệt mà upload thì chuyển về PENDING
            if (ListingStatus.APPROVED.equals(coSo.getTrangThai())) {
                coSo.setTrangThai(ListingStatus.PENDING);
                coSo.setNgayGuiDuyet(LocalDateTime.now());
                coSo.setNgayCapNhat(LocalDateTime.now());
                coSoRepo.save(coSo);
            }

            int newImageCount = (int) (isImage ? imageCount + 1 : imageCount);
            int newVideoCount = (int) (!isImage ? videoCount + 1 : videoCount);
            int mediaId = savedMedia.getMediaId() != null ? savedMedia.getMediaId() : 0;

            return new MediaDtos.MediaUploadResponse(mediaId, url, normType, media.getKichThuoc(), media.getThuTu(), newImageCount, newVideoCount);
        } catch (AppException e) {
            storageService.delete(url);
            throw e;
        } catch (Exception e) {
            storageService.delete(url);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Lỗi khi lưu media vào cơ sở dữ liệu.");
        }
    }

    public MediaDtos.MediaDeleteResponse delete(int maCSLT, int mediaId, int hostId) {
        CoSoLuuTru coSo = coSoRepo.findByIdForUpdate(maCSLT)
                .orElseThrow(() -> new AppException(ErrorCode.LISTING_NOT_FOUND));

        if (coSo.getHost() == null || !coSo.getHost().getId().equals(hostId)) {
            throw new AppException(ErrorCode.NOT_OWNER);
        }
        if (ListingStatus.PENDING.equals(coSo.getTrangThai())) {
            throw new AppException(ErrorCode.LISTING_LOCKED_PENDING);
        }
        if (ListingStatus.REMOVED.equals(coSo.getTrangThai())) {
            throw new AppException(ErrorCode.LISTING_REMOVED);
        }

        Media media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        if (media.getCoSo() == null || !media.getCoSo().getMaCSLT().equals(maCSLT)) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND);
        }

        List<Media> currentMedia = coSo.getMedia() != null ? coSo.getMedia() : List.of();
        long imageCount = currentMedia.stream().filter(m -> "IMAGE".equalsIgnoreCase(m.getLoai())).count();
        long videoCount = currentMedia.stream().filter(m -> "VIDEO".equalsIgnoreCase(m.getLoai())).count();

        // Quy tắc: CSLT không phải DRAFT thì không được xóa làm còn < 3 ảnh
        if (!ListingStatus.DRAFT.equals(coSo.getTrangThai()) && "IMAGE".equalsIgnoreCase(media.getLoai()) && imageCount <= MIN_IMAGES) {
            throw new AppException(ErrorCode.MIN_IMAGES_REQUIRED);
        }

        String url = media.getDuongDan();

        // Xóa trong DB trước, flush để đảm bảo thành công trước khi xóa file vật lý
        mediaRepo.delete(media);
        mediaRepo.flush();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        storageService.delete(url);
                    } catch (Exception e) {
                    }
                }
            });
        } else {
            try {
                storageService.delete(url);
            } catch (Exception ignored) {
            }
        }

        boolean thayDoiTrangThai = false;
        // Quy tắc nghiệp vụ từ API Design: CSLT đã duyệt mà xóa media thì chuyển về PENDING
        if (ListingStatus.APPROVED.equals(coSo.getTrangThai())) {
            coSo.setTrangThai(ListingStatus.PENDING);
            coSo.setNgayGuiDuyet(LocalDateTime.now());
            coSo.setNgayCapNhat(LocalDateTime.now());
            coSoRepo.save(coSo);
            thayDoiTrangThai = true;
        }

        int newImageCount = (int) ("IMAGE".equalsIgnoreCase(media.getLoai()) ? Math.max(0, imageCount - 1) : imageCount);
        int newVideoCount = (int) ("VIDEO".equalsIgnoreCase(media.getLoai()) ? Math.max(0, videoCount - 1) : videoCount);

        return new MediaDtos.MediaDeleteResponse("Xóa file thành công", newImageCount, newVideoCount, thayDoiTrangThai);
    }

    private void validateFileType(MultipartFile file, String type) {
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "";
        filename = filename.toLowerCase();

        boolean isImage = "IMAGE".equals(type);
        if (isImage) {
            if (!filename.endsWith(".jpg") && !filename.endsWith(".jpeg") && !filename.endsWith(".png") && !filename.endsWith(".webp")) {
                throw new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
        } else if ("VIDEO".equals(type)) {
            if (!filename.endsWith(".mp4")) {
                throw new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header, 0, 12);
            if (read < 4) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "File không hợp lệ hoặc bị hỏng.");
            }
            String hex = bytesToHex(java.util.Arrays.copyOf(header, read));
            // Header 'MZ' (4D5A) của file thực thi .exe/.dll
            if (hex.startsWith("4D5A")) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Phát hiện mã độc giả mạo định dạng file.");
            }

            // Kiểm tra Magic Bytes đặc trưng theo từng định dạng
            if (isImage) {
                boolean isJpeg = (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) && hex.startsWith("FFD8FF");
                boolean isPng = filename.endsWith(".png") && hex.startsWith("89504E47");
                boolean isWebp = filename.endsWith(".webp") && read >= 12 && hex.startsWith("52494646") && hex.substring(16, 24).equalsIgnoreCase("57454250");
                if (!isJpeg && !isPng && !isWebp) {
                    throw new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Định dạng nội dung file không khớp với phần mở rộng.");
                }
            } else if ("VIDEO".equals(type)) {
                // MP4: box ftyp ở byte 4-7 (hex characters 8-15)
                boolean isMp4 = read >= 8 && hex.length() >= 16 && hex.substring(8, 16).equalsIgnoreCase("66747970");
                if (!isMp4) {
                    throw new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Định dạng nội dung file không phải MP4 hợp lệ.");
                }
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "File không hợp lệ hoặc bị hỏng.");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
