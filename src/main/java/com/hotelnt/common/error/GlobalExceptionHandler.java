package com.hotelnt.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleApp(AppException e) {
        ErrorCode c = e.getCode();
        return ResponseEntity.status(c.getStatus())
                .body(ApiErrorResponse.of(c.name(), c.getMessage(), e.getDetails()));
    }

    /** Loi validate @ModelAttribute / @Valid (kieu sai, vuot do dai, ...) */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBind(BindException e) {
        List<Map<String, String>> details = e.getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message",
                        f.getDefaultMessage() == null ? "Không hợp lệ" : f.getDefaultMessage()))
                .toList();
        return validation(details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException e) {
        List<Map<String, String>> details = e.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList();
        return validation(details);
    }

    /** Vi du /listings/abc (maCSLT phai la so) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return validation(List.of(Map.of("field", e.getName(), "message", "Sai định dạng")));
    }

    /** Thieu tham so bat buoc, vi du map-markers thieu boundsNorthEast */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return validation(List.of(Map.of("field", e.getParameterName(), "message", "Thiếu tham số bắt buộc")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiErrorResponse.of("METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.", null));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", "Đường dẫn không tồn tại.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception e) {
        log.error("Unhandled error", e);
        ErrorCode c = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(c.getStatus()).body(ApiErrorResponse.of(c.name(), c.getMessage(), null));
    }

    private ResponseEntity<ApiErrorResponse> validation(Object details) {
        ErrorCode c = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(c.getStatus()).body(ApiErrorResponse.of(c.name(), c.getMessage(), details));
    }
}
