package com.hotelnt.common.error;

import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleMaxUploadSize: B?t MaxUploadSizeExceededException, tr? v? HTTP 413 v� ApiErrorResponse chu?n FILE_TOO_LARGE")
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