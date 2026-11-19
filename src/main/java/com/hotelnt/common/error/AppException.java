package com.hotelnt.common.error;

/**
 * Nem exception nay o bat ky dau; GlobalExceptionHandler se doi thanh JSON loi
 * chuan.
 */
public class AppException extends RuntimeException {
    private final ErrorCode code;
    private final transient Object details;

    public AppException(ErrorCode code) {
        this(code, null);
    }

    public AppException(ErrorCode code, Object details) {
        super(code.getMessage());
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }
}
