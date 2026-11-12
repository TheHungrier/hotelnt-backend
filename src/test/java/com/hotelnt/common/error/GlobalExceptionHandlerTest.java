package com.hotelnt.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleMaxUploadSize: Bắt MaxUploadSizeExceededException, trả về HTTP 413 và ApiErrorResponse chuẩn FILE_TOO_LARGE")
    void handleMaxUploadSize_ReturnsPayloadTooLargeAndStandardBody() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(105 * 1024 * 1024);

        ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSize(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(413, response.getStatusCode().value());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.success());
        assertNotNull(body.error());
        assertEquals("FILE_TOO_LARGE", body.error().code());
        assertEquals(ErrorCode.FILE_TOO_LARGE.getMessage(), body.error().message());
        assertNull(body.error().details());
    }
}