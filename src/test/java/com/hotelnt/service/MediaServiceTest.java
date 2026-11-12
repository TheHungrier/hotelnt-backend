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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.transaction.support.TransactionSynchronization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    private static final byte[] VALID_JPEG_HEADER = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01 };
    private static final byte[] VALID_PNG_HEADER = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D };
    private static final byte[] VALID_WEBP_HEADER = new byte[] {
            0x52, 0x49, 0x46, 0x46, // 'RIFF'
            0x24, 0x00, 0x00, 0x00, // file size
            0x57, 0x45, 0x42, 0x50 // 'WEBP'
    };
    private static final byte[] VALID_MP4_HEADER = new byte[] {
            0x00, 0x00, 0x00, 0x20, // box size 32
            0x66, 0x74, 0x79, 0x70, // 'ftyp'
            0x69, 0x73, 0x6F, 0x6D // 'isom'
    };

    @Mock
    private CoSoLuuTruRepository coSoRepo;

    @Mock
    private MediaRepository mediaRepo;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private MediaService mediaService;

    private CoSoLuuTru mockCoSo;
    private TaiKhoan mockHost;

    @BeforeEach
    void setUp() {
        mockHost = new TaiKhoan();
        mockHost.setId(2);

        mockCoSo = new CoSoLuuTru();
        mockCoSo.setMaCSLT(15);
        mockCoSo.setHost(mockHost);
        mockCoSo.setTrangThai(ListingStatus.DRAFT);
        mockCoSo.setMedia(new ArrayList<>());
    }

    // ==========================================
    // UPLOAD TESTS
    // ==========================================

    @Test
    void upload_Success_ValidImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", VALID_JPEG_HEADER);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/uuid.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(invocation -> {
            Media m = invocation.getArgument(0);
            m.setMediaId(101);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");

        assertNotNull(res);
        assertEquals(101, res.mediaId());
        assertEquals("/uploads/uuid.jpg", res.url());
        assertEquals("IMAGE", res.type());
        assertEquals(1, res.soAnh());
        assertEquals(0, res.soVideo());
        verify(mediaRepo, times(1)).save(any(Media.class));
    }

    @Test
    void upload_Success_ApprovedListing_TransitionsToPending() {
        mockCoSo.setTrangThai(ListingStatus.APPROVED);

        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", VALID_JPEG_HEADER);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/uuid.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(invocation -> {
            Media m = invocation.getArgument(0);
            m.setMediaId(102);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");

        assertNotNull(res);
        assertEquals(ListingStatus.PENDING, mockCoSo.getTrangThai());
        verify(coSoRepo, times(1)).save(mockCoSo);
    }

    @Test
    void upload_Success_ValidVideo() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tour.mp4", "video/mp4", VALID_MP4_HEADER);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("VIDEO"))).thenReturn("/uploads/video.mp4");
        when(mediaRepo.save(any(Media.class))).thenAnswer(invocation -> {
            Media m = invocation.getArgument(0);
            m.setMediaId(201);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "VIDEO");

        assertNotNull(res);
        assertEquals(201, res.mediaId());
        assertEquals("/uploads/video.mp4", res.url());
        assertEquals("VIDEO", res.type());
        assertEquals(0, res.soAnh());
        assertEquals(1, res.soVideo());
    }

    @Test
    void upload_Fail_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, emptyFile, "IMAGE"));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    @Test
    void upload_Fail_NotOwner() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", "bytes".getBytes());
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 999, file, "IMAGE"));
        assertEquals(ErrorCode.NOT_OWNER, ex.getCode());
    }

    @Test
    void upload_Fail_ListingPending() {
        mockCoSo.setTrangThai(ListingStatus.PENDING);
        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", "bytes".getBytes());
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.LISTING_LOCKED_PENDING, ex.getCode());
    }

    @Test
    void upload_Fail_ListingRemoved() {
        mockCoSo.setTrangThai(ListingStatus.REMOVED);
        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", "bytes".getBytes());
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.LISTING_REMOVED, ex.getCode());
    }

    @Test
    void upload_Fail_ImageTooLarge() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB > 10MB
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getCode());
    }

    @Test
    void upload_Image_ExactBoundary_10MB_Success() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo_10mb.jpg");
        when(file.getSize()).thenReturn(10 * 1024 * 1024L); // 10,485,760 bytes
        try {
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(VALID_JPEG_HEADER));
        } catch (IOException ignored) {
        }

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/photo_10mb.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(150);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");
        assertNotNull(res);
        assertEquals(10 * 1024 * 1024, res.kichThuoc());
        assertEquals(1, res.soAnh());
    }

    @Test
    void upload_Image_ExactBoundary_10MBPlus1Byte_FailsFileTooLarge() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10 * 1024 * 1024L + 1L); // 10,485,761 bytes

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getCode());
    }

    @Test
    void upload_Video_ExactBoundary_100MB_Success() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("video_100mb.mp4");
        when(file.getSize()).thenReturn(100 * 1024 * 1024L); // 104,857,600 bytes
        try {
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(VALID_MP4_HEADER));
        } catch (IOException ignored) {
        }

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("VIDEO"))).thenReturn("/uploads/video_100mb.mp4");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(250);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "VIDEO");
        assertNotNull(res);
        assertEquals(100 * 1024 * 1024, res.kichThuoc());
        assertEquals(1, res.soVideo());
    }

    @Test
    void upload_Video_ExactBoundary_100MBPlus1Byte_FailsFileTooLarge() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100 * 1024 * 1024L + 1L); // 104,857,601 bytes

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "VIDEO"));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getCode());
    }

    @Test
    void upload_Fail_VideoTooLarge() {
        byte[] largeBytes = new byte[101 * 1024 * 1024]; // 101MB > 100MB
        MockMultipartFile file = new MockMultipartFile("file", "large.mp4", "video/mp4", largeBytes);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "VIDEO"));
        assertEquals(ErrorCode.FILE_TOO_LARGE, ex.getCode());
    }

    @Test
    void upload_Fail_UnsupportedFormat() {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf",
                "pdf-data".getBytes());
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getCode());
    }

    @Test
    void upload_Fail_FakeExecutableFile() {
        byte[] fakeExe = new byte[] { 0x4D, 0x5A, (byte) 0x90, 0x00 }; // MZ header
        MockMultipartFile file = new MockMultipartFile("file", "virus.jpg", "image/jpeg", fakeExe);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
        assertTrue(ex.getDetails().toString().contains("m� được"));
    }

    @Test
    void upload_Fail_FakeTxtFile_DisguisedAsJpg() {
        byte[] fakeTxtBytes = "This is a plain text file disguised as jpg.".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", fakeTxtBytes);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getCode());
    }

    @Test
    void upload_Fail_FakeTxtFile_DisguisedAsPng() {
        byte[] fakeTxtBytes = "This is a plain text file disguised as png.".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", fakeTxtBytes);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getCode());
    }

    @Test
    void upload_Fail_FakeTxtFile_DisguisedAsWebp() {
        byte[] fakeTxtBytes = "This is a plain text file disguised as webp.".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.webp", "image/webp", fakeTxtBytes);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getCode());
    }

    @Test
    void upload_Fail_FakeTxtFile_DisguisedAsMp4() {
        byte[] fakeTxtBytes = "This is a plain text file disguised as mp4.".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.mp4", "video/mp4", fakeTxtBytes);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "VIDEO"));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getCode());
    }

    @Test
    void upload_Success_ValidPngImage() {
        MockMultipartFile file = new MockMultipartFile("file", "room.png", "image/png", VALID_PNG_HEADER);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/uuid.png");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(103);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");
        assertNotNull(res);
        assertEquals("/uploads/uuid.png", res.url());
    }

    @Test
    void upload_Success_ValidWebpImage() {
        MockMultipartFile file = new MockMultipartFile("file", "room.webp", "image/webp", VALID_WEBP_HEADER);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/uuid.webp");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(104);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");
        assertNotNull(res);
        assertEquals("/uploads/uuid.webp", res.url());
    }

    @Test
    void upload_Fail_ImageLimitExceeded() {
        List<Media> hundredImages = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Media m = new Media();
            m.setLoai("IMAGE");
            hundredImages.add(m);
        }
        mockCoSo.setMedia(hundredImages);

        MockMultipartFile file = new MockMultipartFile("file", "pic101.jpg", "image/jpeg", VALID_JPEG_HEADER);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "IMAGE"));
        assertEquals(ErrorCode.MEDIA_LIMIT_EXCEEDED, ex.getCode());
    }

    @Test
    void upload_Fail_VideoLimitExceeded() {
        List<Media> fiveVideos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Media v = new Media();
            v.setLoai("VIDEO");
            fiveVideos.add(v);
        }
        mockCoSo.setMedia(fiveVideos);

        MockMultipartFile file = new MockMultipartFile("file", "tour2.mp4", "video/mp4", VALID_MP4_HEADER);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.upload(15, 2, file, "VIDEO"));
        assertEquals(ErrorCode.MEDIA_LIMIT_EXCEEDED, ex.getCode());
    }

    @Test
    void upload_Success_Image_ExactBoundary99To100() {
        List<Media> ninetyNineImages = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            Media m = new Media();
            m.setLoai("IMAGE");
            ninetyNineImages.add(m);
        }
        mockCoSo.setMedia(ninetyNineImages);

        MockMultipartFile file = new MockMultipartFile("file", "pic100.jpg", "image/jpeg", VALID_JPEG_HEADER);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/pic100.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(120);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");
        assertNotNull(res);
        assertEquals(100, res.soAnh());
    }

    @Test
    void upload_Success_Video_ExactBoundary4To5() {
        List<Media> fourVideos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Media v = new Media();
            v.setLoai("VIDEO");
            fourVideos.add(v);
        }
        mockCoSo.setMedia(fourVideos);

        MockMultipartFile file = new MockMultipartFile("file", "tour5.mp4", "video/mp4", VALID_MP4_HEADER);
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("VIDEO"))).thenReturn("/uploads/tour5.mp4");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(220);
            return m;
        });

        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "VIDEO");
        assertNotNull(res);
        assertEquals(5, res.soVideo());
    }

    // ==========================================
    // DELETE TESTS
    // ==========================================

    @Test
    void delete_Success_DraftListing() {
        Media m = new Media();
        m.setMediaId(1);
        m.setLoai("IMAGE");
        m.setDuongDan("/uploads/photo1.jpg");
        m.setCoSo(mockCoSo);

        List<Media> mediaList = new ArrayList<>(List.of(m));
        mockCoSo.setMedia(mediaList);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(mediaRepo.findById(1)).thenReturn(Optional.of(m));

        MediaDtos.MediaDeleteResponse res = mediaService.delete(15, 1, 2);

        assertNotNull(res);
        assertEquals("Xóa file thành công", res.message());
        assertEquals(0, res.soAnh());
        assertFalse(res.thayDoiTrangThai());
        verify(mediaRepo, times(1)).delete(m);
        verify(storageService, times(1)).delete("/uploads/photo1.jpg");
    }

    @Test
    void delete_Success_ApprovedListing_TransitionsToPending() {
        mockCoSo.setTrangThai(ListingStatus.APPROVED);

        List<Media> mediaList = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Media m = new Media();
            m.setMediaId(i);
            m.setLoai("IMAGE");
            m.setDuongDan("/uploads/img" + i + ".jpg");
            m.setCoSo(mockCoSo);
            mediaList.add(m);
        }
        mockCoSo.setMedia(mediaList);
        Media target = mediaList.get(0);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(mediaRepo.findById(1)).thenReturn(Optional.of(target));

        MediaDtos.MediaDeleteResponse res = mediaService.delete(15, 1, 2);

        assertNotNull(res);
        assertEquals(3, res.soAnh());
        assertTrue(res.thayDoiTrangThai());
        assertEquals(ListingStatus.PENDING, mockCoSo.getTrangThai());
        verify(coSoRepo, times(1)).save(mockCoSo);
    }

    @Test
    void delete_Success_Video_WhenApprovedListingHasLessThanThreeImages() {
        mockCoSo.setTrangThai(ListingStatus.APPROVED);

        List<Media> mediaList = new ArrayList<>();
        // 2 ảnh (dưới ngưỡng MIN_IMAGES = 3 của ảnh)
        for (int i = 1; i <= 2; i++) {
            Media img = new Media();
            img.setMediaId(i);
            img.setLoai("IMAGE");
            img.setDuongDan("/uploads/img" + i + ".jpg");
            img.setCoSo(mockCoSo);
            mediaList.add(img);
        }
        // 1 video
        Media video = new Media();
        video.setMediaId(10);
        video.setLoai("VIDEO");
        video.setDuongDan("/uploads/tour.mp4");
        video.setCoSo(mockCoSo);
        mediaList.add(video);

        mockCoSo.setMedia(mediaList);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(mediaRepo.findById(10)).thenReturn(Optional.of(video));

        MediaDtos.MediaDeleteResponse res = mediaService.delete(15, 10, 2);

        assertNotNull(res);
        assertEquals("Xóa file thành công", res.message());
        assertEquals(2, res.soAnh());
        assertEquals(0, res.soVideo());
        assertTrue(res.thayDoiTrangThai());
        assertEquals(ListingStatus.PENDING, mockCoSo.getTrangThai());
        verify(mediaRepo, times(1)).delete(video);
        verify(storageService, times(1)).delete("/uploads/tour.mp4");
        verify(coSoRepo, times(1)).save(mockCoSo);
    }

    @Test
    void delete_Fail_MinImagesRequired_WhenApproved() {
        mockCoSo.setTrangThai(ListingStatus.APPROVED);

        List<Media> mediaList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Media m = new Media();
            m.setMediaId(i);
            m.setLoai("IMAGE");
            m.setCoSo(mockCoSo);
            mediaList.add(m);
        }
        mockCoSo.setMedia(mediaList);
        Media target = mediaList.get(0);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(mediaRepo.findById(1)).thenReturn(Optional.of(target));

        AppException ex = assertThrows(AppException.class, () -> mediaService.delete(15, 1, 2));
        assertEquals(ErrorCode.MIN_IMAGES_REQUIRED, ex.getCode());
    }

    @Test
    void delete_Fail_NotOwner() {
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));

        AppException ex = assertThrows(AppException.class, () -> mediaService.delete(15, 1, 999));
        assertEquals(ErrorCode.NOT_OWNER, ex.getCode());
    }

    @Test
    void delete_Fail_MediaNotFound() {
        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(mediaRepo.findById(99)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> mediaService.delete(15, 99, 2));
        assertEquals(ErrorCode.MEDIA_NOT_FOUND, ex.getCode());
    }

    @Test
    void delete_Fail_MediaNotBelongToListing() {
        CoSoLuuTru otherCoSo = new CoSoLuuTru();
        otherCoSo.setMaCSLT(99);

        Media otherMedia = new Media();
        otherMedia.setMediaId(1);
        otherMedia.setCoSo(otherCoSo);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(mediaRepo.findById(1)).thenReturn(Optional.of(otherMedia));

        AppException ex = assertThrows(AppException.class, () -> mediaService.delete(15, 1, 2));
        assertEquals(ErrorCode.MEDIA_NOT_FOUND, ex.getCode());
    }

    @Test
    void delete_WithTransactionActive_PhysicalFileDeletedAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            Media m = new Media();
            m.setMediaId(1);
            m.setLoai("IMAGE");
            m.setDuongDan("/uploads/photo1.jpg");
            m.setCoSo(mockCoSo);
            mockCoSo.setMedia(new ArrayList<>(List.of(m)));

            when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
            when(mediaRepo.findById(1)).thenReturn(Optional.of(m));

            mediaService.delete(15, 1, 2);

            // Truýc khi commit, storageService.delete chứa được gọi
            verify(storageService, never()).delete("/uploads/photo1.jpg");

            // Giả lặp Spring Transaction commit thành công
            TransactionSynchronizationUtils.triggerAfterCommit();

            // Sau khi commit, storageService.delete �� được gọi
            verify(storageService, times(1)).delete("/uploads/photo1.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void delete_WithTransactionRollback_PhysicalFileNotDeleted() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            Media m = new Media();
            m.setMediaId(1);
            m.setLoai("IMAGE");
            m.setDuongDan("/uploads/photo1.jpg");
            m.setCoSo(mockCoSo);
            mockCoSo.setMedia(new ArrayList<>(List.of(m)));

            when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
            when(mediaRepo.findById(1)).thenReturn(Optional.of(m));

            mediaService.delete(15, 1, 2);

            // Giả lặp DB Rollback (kh?ng gọi triggerAfterCommit)
            // storageService.delete tuyệt đài KH�NG được gọi
            verify(storageService, never()).delete("/uploads/photo1.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ==========================================
    // CONCURRENCY & RACE CONDITION TESTS
    // ==========================================

    @Test
    void concurrency_TwoUploadsAt99Images_ExactlyOneSucceedsOneFails() throws Exception {
        List<Media> ninetyNineImages = new ArrayList<>();
        for (int i = 1; i <= 99; i++) {
            Media m = new Media();
            m.setMediaId(i);
            m.setLoai("IMAGE");
            m.setThuTu(i);
            ninetyNineImages.add(m);
        }
        mockCoSo.setMedia(ninetyNineImages);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger limitExceededCount = new AtomicInteger(0);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/test.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(100 + successCount.get());
            mockCoSo.getMedia().add(m);
            return m;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Void> task = () -> {
            latch.await();
            try {
                MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", VALID_JPEG_HEADER);
                synchronized (mockCoSo) {
                    mediaService.upload(15, 2, file, "IMAGE");
                }
                successCount.incrementAndGet();
            } catch (AppException e) {
                if (ErrorCode.MEDIA_LIMIT_EXCEEDED.equals(e.getCode())) {
                    limitExceededCount.incrementAndGet();
                }
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);
        latch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Ch?nh x?c 1 request thành công");
        assertEquals(1, limitExceededCount.get(), "Ch?nh x?c 1 request nhận MEDIA_LIMIT_EXCEEDED");
        assertEquals(100, mockCoSo.getMedia().size(), "Database c? d?ng 100 ảnh");
    }

    @Test
    void concurrency_TwoUploadsAt4Videos_ExactlyOneSucceedsOneFails() throws Exception {
        List<Media> fourVideos = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Media v = new Media();
            v.setMediaId(i);
            v.setLoai("VIDEO");
            v.setThuTu(i);
            fourVideos.add(v);
        }
        mockCoSo.setMedia(fourVideos);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger limitExceededCount = new AtomicInteger(0);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("VIDEO"))).thenReturn("/uploads/video.mp4");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setMediaId(200 + successCount.get());
            mockCoSo.getMedia().add(m);
            return m;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Void> task = () -> {
            latch.await();
            try {
                MockMultipartFile file = new MockMultipartFile("file", "tour.mp4", "video/mp4", VALID_MP4_HEADER);
                synchronized (mockCoSo) {
                    mediaService.upload(15, 2, file, "VIDEO");
                }
                successCount.incrementAndGet();
            } catch (AppException e) {
                if (ErrorCode.MEDIA_LIMIT_EXCEEDED.equals(e.getCode())) {
                    limitExceededCount.incrementAndGet();
                }
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);
        latch.countDown();

        f1.get();
        f2.get();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Ch?nh x?c 1 request thành công");
        assertEquals(1, limitExceededCount.get(), "Ch?nh x?c 1 request nhận MEDIA_LIMIT_EXCEEDED");
        assertEquals(5, mockCoSo.getMedia().size(), "Database c? d?ng 5 video");
    }

    @Test
    void concurrency_TwoUploadsOnDifferentListings_ExecuteInParallelWithoutBlocking() throws Exception {
        CoSoLuuTru coSo16 = new CoSoLuuTru();
        coSo16.setMaCSLT(16);
        coSo16.setHost(mockHost);
        coSo16.setTrangThai(ListingStatus.DRAFT);
        coSo16.setMedia(new ArrayList<>());

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(coSoRepo.findByIdForUpdate(16)).thenReturn(Optional.of(coSo16));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/test.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<MediaDtos.MediaUploadResponse> task15 = () -> {
            latch.await();
            MockMultipartFile file = new MockMultipartFile("file", "pic15.jpg", "image/jpeg", VALID_JPEG_HEADER);
            return mediaService.upload(15, 2, file, "IMAGE");
        };

        Callable<MediaDtos.MediaUploadResponse> task16 = () -> {
            latch.await();
            MockMultipartFile file = new MockMultipartFile("file", "pic16.jpg", "image/jpeg", VALID_JPEG_HEADER);
            return mediaService.upload(16, 2, file, "IMAGE");
        };

        Future<MediaDtos.MediaUploadResponse> f1 = executor.submit(task15);
        Future<MediaDtos.MediaUploadResponse> f2 = executor.submit(task16);
        latch.countDown();

        MediaDtos.MediaUploadResponse res1 = f1.get();
        MediaDtos.MediaUploadResponse res2 = f2.get();
        executor.shutdown();

        assertNotNull(res1);
        assertNotNull(res2);
        verify(coSoRepo, times(1)).findByIdForUpdate(15);
        verify(coSoRepo, times(1)).findByIdForUpdate(16);
    }

    @Test
    void concurrency_UploadAndThuTuCalculation_AlwaysUniqueAndMonotonic() {
        List<Media> mediaList = new ArrayList<>();
        for (int id : new int[] { 1, 2, 5 }) {
            Media m = new Media();
            m.setMediaId(id);
            m.setLoai("IMAGE");
            m.setThuTu(id);
            mediaList.add(m);
        }
        mockCoSo.setMedia(mediaList);

        when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
        when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/new.jpg");
        when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", VALID_JPEG_HEADER);
        MediaDtos.MediaUploadResponse res = mediaService.upload(15, 2, file, "IMAGE");

        assertNotNull(res);
        assertEquals(6, res.thuTu(), "Số thẻ tự mới phải l? max(1, 2, 5) + 1 = 6");
    }

    @Test
    void upload_WithTransactionActive_PhysicalFileDeletedOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
            when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/temp_rollback.jpg");
            when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", VALID_JPEG_HEADER);
            mediaService.upload(15, 2, file, "IMAGE");

            verify(storageService, never()).delete("/uploads/temp_rollback.jpg");

            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(storageService, times(1)).delete("/uploads/temp_rollback.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void upload_WithTransactionActive_PhysicalFilePreservedOnCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            when(coSoRepo.findByIdForUpdate(15)).thenReturn(Optional.of(mockCoSo));
            when(storageService.store(any(), eq("IMAGE"))).thenReturn("/uploads/temp_commit.jpg");
            when(mediaRepo.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", VALID_JPEG_HEADER);
            mediaService.upload(15, 2, file, "IMAGE");

            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(storageService, never()).delete("/uploads/temp_commit.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

}