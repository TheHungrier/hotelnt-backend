package com.hotelnt.common.error;

import org.springframework.http.HttpStatus;

/** Ma loi lay tu sheet "Error Format" trong API_DESIGN.xlsx. Them dan khi lam den endpoint tuong ung. */
public enum ErrorCode {
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa."),
    ACCOUNT_DUPLICATED(HttpStatus.CONFLICT, "Email, số điện thoại hoặc CCCD đã được sử dụng."),
    FIELD_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "Email và CCCD không thể tự chỉnh sửa."),
    OLD_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác."),
    LOCATION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Vị trí chọn nằm ngoài khu vực hỗ trợ Nha Trang."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Trạng thái hiện tại của cơ sở không cho phép thao tác này."),
    LISTING_NOT_DELETABLE(HttpStatus.CONFLICT, "Không thể xóa cơ sở ở trạng thái hiện tại."),
    CHECKLIST_INCOMPLETE(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng hoàn thiện các trường bắt buộc trước khi gửi/duyệt."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại các trường được đánh dấu."),
    ACCOUNT_DUPLICATED(HttpStatus.CONFLICT, "Email, số điện thoại hoặc CCCD đã được sử dụng."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Mã OTP không đúng hoặc đã hết hạn."),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn hoặc không còn hiệu lực. Vui lòng yêu cầu mã mới."),
    OTP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai OTP quá 5 lần. Vui lòng yêu cầu mã OTP mới."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác."),
    ACCOUNT_TEMP_LOCKED(HttpStatus.LOCKED, "Tài khoản tạm thời bị khóa. Vui lòng thử lại sau 15 phút."),
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ đủ 60 giây trước khi gửi lại mã."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh. Vui lòng thử lại sau."),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng thực hiện lại từ đầu."),
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
