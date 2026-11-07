package com.hotelnt.common.error;

import org.springframework.http.HttpStatus;

/** Ma loi lay tu sheet "Error Format" trong API_DESIGN.xlsx. Them dan khi lam den endpoint tuong ung. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại các trường được đánh dấu."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."),
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên cơ sở lưu trú này."),
    LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở lưu trú."),
    MEDIA_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Số lượng media đã vượt quá giới hạn cho phép."),
    INVALID_IMAGE_RATIO(HttpStatus.BAD_REQUEST, "Tỷ lệ ảnh không hợp lệ."),
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy media."),
    LISTING_LOCKED_PENDING(HttpStatus.CONFLICT, "Cơ sở lưu trú đang chờ xử lý và tạm thời bị khóa."),
    LISTING_REMOVED(HttpStatus.GONE, "Cơ sở lưu trú đã bị xóa."),
    MIN_IMAGES_REQUIRED(HttpStatus.BAD_REQUEST, "Cần có số lượng ảnh tối thiểu."),
    SLUG_DUPLICATED(HttpStatus.CONFLICT, "Slug đã tồn tại."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Dung lượng file vượt quá giới hạn cho phép."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng media không được hỗ trợ."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp sự cố. Vui lòng thử lại sau."),
    HOST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."),
    STATE_UNCHANGED(HttpStatus.CONFLICT, "Tài khoản đã ở trạng thái này."),
    CANNOT_LOCK_ADMIN(HttpStatus.FORBIDDEN, "Không thể khóa tài khoản Admin."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Trạng thái hiện tại không cho phép thao tác này."),
    CHECKLIST_INCOMPLETE(HttpStatus.UNPROCESSABLE_ENTITY, "Cơ sở chưa đạt điều kiện duyệt."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Chatbot tạm thời không khả dụng.");
    // TODO: OTP_INVALID, OTP_EXPIRED, ACCOUNT_DUPLICATED, ...

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
