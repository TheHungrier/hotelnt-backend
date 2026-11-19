package com.hotelnt.helper;

import com.hotelnt.common.error.ApiErrorResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import com.hotelnt.controller.HostController;

@RestControllerAdvice(assignableTypes = HostController.class)
@Order(-1)
public class HostWebErrors {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> invalidJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Dữ liệu JSON không hợp lệ.", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> concurrentProfileConflict(DataIntegrityViolationException e) {
        String text = e.getMostSpecificCause().getMessage();
        if (text != null && text.contains("UX_TaiKhoan_soDienThoai"))
            return ResponseEntity.status(409)
                    .body(ApiErrorResponse.of("ACCOUNT_DUPLICATED", "Số điện thoại đã được sử dụng.", null));
        return ResponseEntity.internalServerError()
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.", null));
    }
}
