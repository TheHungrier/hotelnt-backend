package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;

final public class ChallengeErrors {
    private ChallengeErrors() {}
    public static void throwIfFailed(ChallengeVerifier.Result result) {
        switch (result.status()) {
            case VALID -> { }
            case EXPIRED -> throw new AppException(ErrorCode.OTP_EXPIRED);
            case INVALID -> throw new AppException(ErrorCode.OTP_INVALID, result.details());
            case ATTEMPTS_EXCEEDED -> throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
    }
}
