package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
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
    // Nh�m A: Luu file v?t l�
    // ==========================================

    @Test
    @DisplayName("store: Luu file ?nh .jpg th�nh c�ng, file du?c ghi xu?ng dia d�ng n?i dung v� k�ch thu?c")
    void store_Success_FileWrittenToDisk() throws IOException {
        byte[] content = "Fake JPEG binary content for testing".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "room-deluxe.jpg", "image/jpeg", content);

        String fileUrl = storageService.store(file, "IMAGE");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        assertTrue(fileUrl.endsWith(".jpg"));

        String savedFilename = fileUrl.substring("/uploads/".length());
        Path savedFilePath = tempDir.resolve(savedFilename);

        assertTrue(Files.exists(savedFilePath), "File v?t l� ph?i t?n t?i tr�n dia trong tempDir");
        assertArrayEquals(content, Files.readAllBytes(savedFilePath), "N?i dung byte tr�n dia ph?i kh?p v?i file g?i v�o");
        assertEquals(content.length, Files.size(savedFilePath), "K�ch thu?c file v?t l� ph?i kh?p ch�nh x�c");
    }

    // ==========================================
    // Nh�m B: T�n file & UUID
    // ==========================================

    @Test
    @DisplayName("store: Hai file c�ng t�n g?c t?o ra 2 UUID d?c l?p, c? hai c�ng t?n t?i v� kh�ng b? ghi d�")
    void store_GeneratesUniqueUuidFilename() throws IOException {
        byte[] content1 = "File 1 unique content".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "File 2 different content".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file1 = new MockMultipartFile("file", "room.jpg", "image/jpeg", content1);
        MockMultipartFile file2 = new MockMultipartFile("file", "room.jpg", "image/jpeg", content2);

        String url1 = storageService.store(file1, "IMAGE");
        String url2 = storageService.store(file2, "IMAGE");

        assertNotNull(url1);
        assertNotNull(url2);
        assertNotEquals(url1, url2, "URL tr? v? ph?i mang 2 UUID kh�c nhau");

        Path path1 = tempDir.resolve(url1.substring("/uploads/".length()));
        Path path2 = tempDir.resolve(url2.substring("/uploads/".length()));

        assertTrue(Files.exists(path1), "File 1 ph?i t?n t?i tr�n dia");
        assertTrue(Files.exists(path2), "File 2 ph?i t?n t?i tr�n dia");
        assertArrayEquals(content1, Files.readAllBytes(path1));
        assertArrayEquals(content2, Files.readAllBytes(path2));
    }

    @Test
    @DisplayName("store: File kh�ng c� ph?n m? r?ng du?c luu th�nh c�ng v?i t�n UUID kh�ng c� du�i")
    void store_FileWithoutExtension() throws IOException {
        byte[] content = "Raw binary without extension".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "noextension", "application/octet-stream", content);

        String fileUrl = storageService.store(file, "IMAGE");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/uploads/"));
        String filename = fileUrl.substring("/uploads/".length());
        assertFalse(filename.contains("."), "T�n file luu tr? kh�ng du?c ch?a d?u ch?m ph?n m? r?ng");

        Path savedFilePath = tempDir.resolve(filename);
        assertTrue(Files.exists(savedFilePath));
        assertArrayEquals(content, Files.readAllBytes(savedFilePath));
    }

    // ==========================================
    // Nh�m C: Thu m?c luu tr?
    // ==========================================

    @Test
    @DisplayName("constructor: T? d?ng t?o thu m?c con tr�n dia n?u chua t?n t?i")
    void constructor_CreatesDirectoryIfNotExists() {
        Path nestedDir = tempDir.resolve("nested").resolve("sub-uploads");
        assertFalse(Files.exists(nestedDir), "Thu m?c chua du?c t?o tru?c khi kh?i t?o service");

        LocalFileStorageService customStorageService = new LocalFileStorageService(nestedDir.toString());

        assertNotNull(customStorageService);
        assertTrue(Files.exists(nestedDir), "Thu m?c ph?i du?c t? d?ng t?o sau khi kh?i t?o service");
        assertTrue(Files.isDirectory(nestedDir), "�u?ng d?n du?c t?o ph?i l� thu m?c");
    }

    // ==========================================
    // Nh�m D: X�a file
    // ==========================================

    @Test
    @DisplayName("delete: X�a file th�nh c�ng khi URL h?p l? v� file t?n t?i tr�n dia")
    void delete_Success_FileDeletedFromDisk() {
        byte[] content = "File to be deleted".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "delete-me.png", "image/png", content);
        String fileUrl = storageService.store(file, "IMAGE");

        Path savedFilePath = tempDir.resolve(fileUrl.substring("/uploads/".length()));
        assertTrue(Files.exists(savedFilePath), "File ph?i t?n t?i tru?c khi x�a");

        storageService.delete(fileUrl);

        assertFalse(Files.exists(savedFilePath), "File v?t l� ph?i b? x�a ho�n to�n kh?i dia");
    }

    @Test
    @DisplayName("delete: File kh�ng t?n t?i du?c x? l� an to�n (gracefully), kh�ng throw exception")
    void delete_NonExistentFile_HandledGracefully() {
        assertDoesNotThrow(() -> storageService.delete("/uploads/non-existent-uuid-12345.jpg"));
    }

    @Test
    @DisplayName("delete: URL kh�ng d�ng d?nh d?ng (null, r?ng, sai ti?n t?) du?c b? qua an to�n")
    void delete_InvalidUrlFormat_Ignored() {
        assertDoesNotThrow(() -> storageService.delete(null));
        assertDoesNotThrow(() -> storageService.delete(""));
        assertDoesNotThrow(() -> storageService.delete("   "));
        assertDoesNotThrow(() -> storageService.delete("/other/directory/file.jpg"));
        assertDoesNotThrow(() -> storageService.delete("https://cdn.example.com/images/sample.png"));
    }

    // ==========================================
    // Nh�m E: An to�n du?ng d?n (Path Traversal)
    // ==========================================

    @Test
    @DisplayName("store: Ph�t hi?n t?n c�ng Path Traversal trong t�n file v� n�m VALIDATION_ERROR")
    void store_PathTraversalAttempt_ThrowsValidationError() {
        byte[] content = "Malicious file content".getBytes(StandardCharsets.UTF_8);
        // T�n file ch?a path traversal khi?n destinationFile kh�ng thu?c rootLocation
        MockMultipartFile maliciousFile = new MockMultipartFile("file", "file.jpg/../../evil", "image/jpeg", content);

        AppException ex = assertThrows(AppException.class, () -> storageService.store(maliciousFile, "IMAGE"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
        assertEquals("Ph�t hi?n truy c?p du?ng d?n kh�ng h?p l?", ex.getDetails());
    }

    @Test
    @DisplayName("delete: Path Traversal attempt kh�ng du?c ph�p x�a file n?m ngo�i thu m?c rootLocation")
    void delete_PathTraversalAttempt_DoesNotDeleteOutside() throws IOException {
        Path outsideDir = tempDir.getParent();
        assertNotNull(outsideDir, "tempDir ph?i c� thu m?c cha");
        Path outsideFile = outsideDir.resolve("canary-file-" + System.currentTimeMillis() + ".txt");

        try {
            Files.write(outsideFile, "Secret Canary Content".getBytes(StandardCharsets.UTF_8));
            assertTrue(Files.exists(outsideFile), "Canary file ph?i t?n t?i ngo�i tempDir tru?c test");

            // Th? g?i delete v?i path traversal l�i ra ngo�i thu m?c uploads
            String maliciousUrl = "/uploads/../" + outsideFile.getFileName().toString();
            storageService.delete(maliciousUrl);

            // File b�n ngo�i v?n ph?i t?n t?i nguy�n v?n
            assertTrue(Files.exists(outsideFile), "File ngo�i thu m?c rootLocation tuy?t d?i kh�ng b? x�a");
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    // ==========================================
    // Nh�m F: C�c t�nh hu?ng l?i
    // ==========================================

    @Test
    @DisplayName("store: File r?ng (0 bytes) n�m VALIDATION_ERROR v?i message 'File r?ng'")
    void store_EmptyFile_ThrowsValidationError() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        AppException ex = assertThrows(AppException.class, () -> storageService.store(emptyFile, "IMAGE"));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
        assertEquals("File r?ng", ex.getDetails());
    }

    @Test
    @DisplayName("store: Khi InputStream n�m IOException th� chuy?n th�nh INTERNAL_ERROR")
    void store_InputStreamThrowsIOException_ThrowsInternalError() throws IOException {
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(brokenFile.getInputStream()).thenThrow(new IOException("Simulated disk read failure"));

        AppException ex = assertThrows(AppException.class, () -> storageService.store(brokenFile, "IMAGE"));

        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getCode());
        assertEquals("L?i khi luu file v?t l�.", ex.getDetails());
    }
}