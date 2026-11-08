package com.hotelnt.modules.auth;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;

final class ChallengeErrors {
    private ChallengeErrors() {}
    static void throwIfFailed(ChallengeVerifier.Result result) {
        switch (result.status()) {
            case VALID -> { }
            case EXPIRED -> throw new AppException(ErrorCode.OTP_EXPIRED);
            case INVALID -> throw new AppException(ErrorCode.OTP_INVALID, result.details());
            case ATTEMPTS_EXCEEDED -> throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
    }
}
