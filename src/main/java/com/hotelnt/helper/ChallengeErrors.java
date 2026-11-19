package com.hotelnt.helper;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;

final public class ChallengeErrors {
    private ChallengeErrors() {
    }

    public static void throwIfFailed(ChallengeVerifier.Result result) {
        switch (result.status()) {
            case VALID -> {
            }
            case EXPIRED -> throw new AppException(ErrorCode.OTP_EXPIRED);
            case INVALID -> throw new AppException(ErrorCode.OTP_INVALID, result.details());
            case ATTEMPTS_EXCEEDED -> throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
    }
}
