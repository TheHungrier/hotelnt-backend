package com.hotelnt.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/** { "success": false, "error": { "code", "message", "details" } } */
public record ApiErrorResponse(boolean success, ErrorBody error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, Object details) {
    }

    public static ApiErrorResponse of(String code, String message, Object details) {
        return new ApiErrorResponse(false, new ErrorBody(code, message, details));
    }
}
