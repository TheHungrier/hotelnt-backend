package com.hotelnt.helper;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;

final public class SigninErrors {
    private SigninErrors() {
    }

    public static void throwIfFailed(CredentialsChecker.Status status, Object details) {
        switch (status) {
            case SUCCESS -> {
            }
            case INVALID -> throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS);
            case TEMP_LOCKED -> throw new AppException(ErrorCode.ACCOUNT_TEMP_LOCKED, details);
            case LOCKED -> throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
    }
}
