package com.hotelnt.modules.media;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString());
    }

    // ==========================================
    // Nhóm A: Lưu file vật lý
    // ==========================================

    @Test
    @DisplayName("store: Lưu file ảnh .jpg thành công, file được ghi xuống đĩa đúng nội dung và kích thước")
    void store_Success_FileWrittenToDisk() throws IOException {
        byte[] content = "Fake JPEG binary content for testing".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "room-deluxe.jpg", "image/jpeg", content);

        String fileUrl = storageService.store(file, "IMAGE");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        assertTrue(fileUrl.endsWith(".jpg"));

        String savedFilename = fileUrl.substring("/uploads/".length());
        Path savedFilePath = tempDir.resolve(savedFilename);

        assertTrue(Files.exists(savedFilePath), "File vật lý phải tồn tại trên đĩa trong tempDir");
        assertArrayEquals(content, Files.readAllBytes(savedFilePath), "Nội dung byte trên đĩa phải khớp với file gửi vào");
        assertEquals(content.length, Files.size(savedFilePath), "Kích thước file vật lý phải khớp chính xác");
    }

    // ==========================================
    // Nhóm B: Tên file & UUID
    // ==========================================

    @Test
    @DisplayName("store: Hai file cùng tên gốc tạo ra 2 UUID độc lập, cả hai cùng tồn tại và không bị ghi đè")
    void store_GeneratesUniqueUuidFilename() throws IOException {
        byte[] content1 = "File 1 unique content".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "File 2 different content".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file1 = new MockMultipartFile("file", "room.jpg", "image/jpeg", content1);
        MockMultipartFile file2 = new MockMultipartFile("file", "room.jpg", "image/jpeg", content2);

        String url1 = storageService.store(file1, "IMAGE");
        String url2 = storageService.store(file2, "IMAGE");

        assertNotNull(url1);
        assertNotNull(url2);
        assertNotEquals(url1, url2, "URL trả về phải mang 2 UUID khác nhau");

        Path path1 = tempDir.resolve(url1.substring("/uploads/".length()));
        Path path2 = tempDir.resolve(url2.substring("/uploads/".length()));

        assertTrue(Files.exists(path1), "File 1 phải tồn tại trên đĩa");
        assertTrue(Files.exists(path2), "File 2 phải tồn tại trên đĩa");
        assertArrayEquals(content1, Files.readAllBytes(path1));
        assertArrayEquals(content2, Files.readAllBytes(path2));
    }

    @Test
    @DisplayName("store: File không có phần mở rộng được lưu thành công với tên UUID không có đuôi")
    void store_FileWithoutExtension() throws IOException {
        byte[] content = "Raw binary without extension".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "noextension", "application/octet-stream", content);

        String fileUrl = storageService.store(file, "IMAGE");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        String filename = fileUrl.substring("/uploads/".length());
        assertFalse(filename.contains("."), "Tên file lưu trữ không được chứa dấu chấm phần mở rộng");

        Path savedFilePath = tempDir.resolve(filename);
        assertTrue(Files.exists(savedFilePath));
        assertArrayEquals(content, Files.readAllBytes(savedFilePath));
    }

    // ==========================================
    // Nhóm C: Thư mục lưu trữ
    // ==========================================

    @Test
    @DisplayName("constructor: Tự động tạo thư mục con trên đĩa nếu chưa tồn tại")
    void constructor_CreatesDirectoryIfNotExists() {
        Path nestedDir = tempDir.resolve("nested").resolve("sub-uploads");
        assertFalse(Files.exists(nestedDir), "Thư mục chưa được tạo trước khi khởi tạo service");

        LocalFileStorageService customStorageService = new LocalFileStorageService(nestedDir.toString());

        assertNotNull(customStorageService);
        assertTrue(Files.exists(nestedDir), "Thư mục phải được tự động tạo sau khi khởi tạo service");
        assertTrue(Files.isDirectory(nestedDir), "Đường dẫn được tạo phải là thư mục");
    }

    // ==========================================
    // Nhóm D: Xóa file
    // ==========================================

    @Test
    @DisplayName("delete: Xóa file thành công khi URL hợp lệ và file tồn tại trên đĩa")
    void delete_Success_FileDeletedFromDisk() {
        byte[] content = "File to be deleted".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "delete-me.png", "image/png", content);
        String fileUrl = storageService.store(file, "IMAGE");

        Path savedFilePath = tempDir.resolve(fileUrl.substring("/uploads/".length()));
        assertTrue(Files.exists(savedFilePath), "File phải tồn tại trước khi xóa");

        storageService.delete(fileUrl);

        assertFalse(Files.exists(savedFilePath), "File vật lý phải bị xóa hoàn toàn khỏi đĩa");
    }

    @Test
    @DisplayName("delete: File không tồn tại được xử lý an toàn (gracefully), không throw exception")
    void delete_NonExistentFile_HandledGracefully() {
        assertDoesNotThrow(() -> storageService.delete("/uploads/non-existent-uuid-12345.jpg"));
    }

    @Test
    @DisplayName("delete: URL không đúng định dạng (null, rỗng, sai tiền tố) được bỏ qua an toàn")
    void delete_InvalidUrlFormat_Ignored() {
        assertDoesNotThrow(() -> storageService.delete(null));
        assertDoesNotThrow(() -> storageService.delete(""));
        assertDoesNotThrow(() -> storageService.delete("   "));
        assertDoesNotThrow(() -> storageService.delete("/other/directory/file.jpg"));
        assertDoesNotThrow(() -> storageService.delete("https://cdn.example.com/images/sample.png"));
    }

    // ==========================================
    // Nhóm E: An toàn đường dẫn (Path Traversal)
    // ==========================================

    @Test
    @DisplayName("store: Phát hiện tấn công Path Traversal trong tên file và ném VALIDATION_ERROR")
    void store_PathTraversalAttempt_ThrowsValidationError() {
        byte[] content = "Malicious file content".getBytes(StandardCharsets.UTF_8);
        // Tên file chứa path traversal khiến destinationFile không thuộc rootLocation
        MockMultipartFile maliciousFile = new MockMultipartFile("file", "file.jpg/../../evil", "image/jpeg", content);

        AppException ex = assertThrows(AppException.class, () -> storageService.store(maliciousFile, "IMAGE"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
        assertEquals("Phát hiện truy cập đường dẫn không hợp lệ", ex.getDetails());
    }

    @Test
    @DisplayName("delete: Path Traversal attempt không được phép xóa file nằm ngoài thư mục rootLocation")
    void delete_PathTraversalAttempt_DoesNotDeleteOutside() throws IOException {
        Path outsideDir = tempDir.getParent();
        assertNotNull(outsideDir, "tempDir phải có thư mục cha");
        Path outsideFile = outsideDir.resolve("canary-file-" + System.currentTimeMillis() + ".txt");

        try {
            Files.write(outsideFile, "Secret Canary Content".getBytes(StandardCharsets.UTF_8));
            assertTrue(Files.exists(outsideFile), "Canary file phải tồn tại ngoài tempDir trước test");

            // Thử gọi delete với path traversal lùi ra ngoài thư mục uploads
            String maliciousUrl = "/uploads/../" + outsideFile.getFileName().toString();
            storageService.delete(maliciousUrl);

            // File bên ngoài vẫn phải tồn tại nguyên vẹn
            assertTrue(Files.exists(outsideFile), "File ngoài thư mục rootLocation tuyệt đối không bị xóa");
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    // ==========================================
    // Nhóm F: Các tình huống lỗi
    // ==========================================

    @Test
    @DisplayName("store: File rỗng (0 bytes) ném VALIDATION_ERROR với message 'File rỗng'")
    void store_EmptyFile_ThrowsValidationError() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        AppException ex = assertThrows(AppException.class, () -> storageService.store(emptyFile, "IMAGE"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
        assertEquals("File rỗng", ex.getDetails());
    }

    @Test
    @DisplayName("store: Khi InputStream ném IOException thì chuyển thành INTERNAL_ERROR")
    void store_InputStreamThrowsIOException_ThrowsInternalError() throws IOException {
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(brokenFile.getInputStream()).thenThrow(new IOException("Simulated disk read failure"));

        AppException ex = assertThrows(AppException.class, () -> storageService.store(brokenFile, "IMAGE"));

        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getCode());
        assertEquals("Lỗi khi lưu file vật lý.", ex.getDetails());
    }
}
