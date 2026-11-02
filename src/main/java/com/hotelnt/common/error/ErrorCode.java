package com.hotelnt.common.error;

import org.springframework.http.HttpStatus;

/** Ma loi lay tu sheet "Error Format" trong API_DESIGN.xlsx. Them dan khi lam den endpoint tuong ung. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại các trường được đánh dấu."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."),
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên cơ sở lưu trú này."),
    LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở lưu trú."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.");
    // TODO: OTP_INVALID, OTP_EXPIRED, ACCOUNT_DUPLICATED, INVALID_STATE_TRANSITION, CHECKLIST_INCOMPLETE, ...

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
