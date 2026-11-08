package com.hotelnt.common.error;

import org.springframework.http.HttpStatus;

/** Ma loi lay tu sheet "Error Format" trong API_DESIGN.xlsx. Them dan khi lam den endpoint tuong ung. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại các trường được đánh dấu."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."),
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên cơ sở lưu trú này."),
    LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở lưu trú."),
    MEDIA_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Vượt quá số lượng cho phép: tối đa 100 ảnh và 5 video cho mỗi cơ sở."),
    INVALID_IMAGE_RATIO(HttpStatus.BAD_REQUEST, "Ảnh phải có tỷ lệ 1.91:1."),
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hình ảnh/video."),
    LISTING_LOCKED_PENDING(HttpStatus.CONFLICT, "Cơ sở đang chờ duyệt nên không thể chỉnh sửa. Hãy rút lại yêu cầu duyệt trước."),
    LISTING_REMOVED(HttpStatus.GONE, "Cơ sở lưu trú đã bị xóa."),
    MIN_IMAGES_REQUIRED(HttpStatus.BAD_REQUEST, "Cơ sở cần có tối thiểu 3 ảnh."),
    SLUG_DUPLICATED(HttpStatus.CONFLICT, "Slug URL đã tồn tại."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp quá lớn: ảnh tối đa 10MB, video tối đa 100MB (ảnh Open Graph tối đa 5MB)."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng không được hỗ trợ: ảnh .jpg/.png/.webp, video .mp4."),
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
