package com.hotelnt.controller;

import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@Import(GlobalExceptionHandler.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private HostIdentityResolver identities;

    @org.junit.jupiter.api.BeforeEach
    void authenticateAsHost() {
        when(identities.resolve(any()))
                .thenReturn(new HostIdentityResolver.Identity(2, "session"));
    }

    @Test
    void uploadMedia_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image-content".getBytes());

        when(mediaService.upload(eq(15), eq(2), any(), eq("IMAGE")))
                .thenReturn(new MediaDtos.MediaUploadResponse(101, "/uploads/uuid.jpg", "IMAGE", 1024, 1, 1, 0));

        mvc.perform(multipart("/host/listings/15/media")
                        .file(file)
                        .param("type", "IMAGE")
                        .header("X-Mock-Host-Id", 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").value(101))
                .andExpect(jsonPath("$.url").value("/uploads/uuid.jpg"))
                .andExpect(jsonPath("$.soAnh").value(1));
    }

    @Test
    void uploadMedia_Fail_UnsupportedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf-data".getBytes());

        when(mediaService.upload(eq(15), eq(2), any(), eq("IMAGE")))
                .thenThrow(new AppException(ErrorCode.UNSUPPORTED_MEDIA_TYPE));

        mvc.perform(multipart("/host/listings/15/media")
                        .file(file)
                        .param("type", "IMAGE")
                        .header("X-Mock-Host-Id", 2))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void uploadMedia_Fail_FileTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "huge.jpg", "image/jpeg", new byte[100]);

        when(mediaService.upload(eq(15), eq(2), any(), eq("IMAGE")))
                .thenThrow(new AppException(ErrorCode.FILE_TOO_LARGE));

        mvc.perform(multipart("/host/listings/15/media")
                        .file(file)
                        .param("type", "IMAGE")
                        .header("X-Mock-Host-Id", 2))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void uploadMedia_Fail_MaxUploadSizeExceeded() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "huge.mp4", "video/mp4", new byte[10]);

        when(mediaService.upload(eq(15), eq(2), any(), eq("VIDEO")))
                .thenThrow(new MaxUploadSizeExceededException(105 * 1024 * 1024));

        mvc.perform(multipart("/host/listings/15/media")
                        .file(file)
                        .param("type", "VIDEO")
                        .header("X-Mock-Host-Id", 2))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    @Test
    void deleteMedia_Success() throws Exception {
        when(mediaService.delete(eq(15), eq(101), eq(2)))
                .thenReturn(new MediaDtos.MediaDeleteResponse("X�a file th�nh c�ng", 2, 0, false));

        mvc.perform(delete("/host/listings/15/media/101")
                        .header("X-Mock-Host-Id", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("X�a file th�nh c�ng"))
                .andExpect(jsonPath("$.soAnh").value(2))
                .andExpect(jsonPath("$.thayDoiTrangThai").value(false));
    }

    @Test
    void deleteMedia_Fail_MinImagesRequired() throws Exception {
        when(mediaService.delete(eq(15), eq(101), eq(2)))
                .thenThrow(new AppException(ErrorCode.MIN_IMAGES_REQUIRED));

        mvc.perform(delete("/host/listings/15/media/101")
                        .header("X-Mock-Host-Id", 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MIN_IMAGES_REQUIRED"));
    }
}