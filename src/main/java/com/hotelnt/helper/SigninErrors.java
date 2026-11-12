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

final public class SigninErrors {
    private SigninErrors() {}
    public static void throwIfFailed(CredentialsChecker.Status status, Object details) {
        switch (status) {
            case SUCCESS -> { }
            case INVALID -> throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS);
            case TEMP_LOCKED -> throw new AppException(ErrorCode.ACCOUNT_TEMP_LOCKED, details);
            case LOCKED -> throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
    }
}
