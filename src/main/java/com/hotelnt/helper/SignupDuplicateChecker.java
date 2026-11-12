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
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Tim tat ca cac truong trung voi tai khoan da ton tai. */
@Service
public class SignupDuplicateChecker {
    private final SignupAccountRepository accounts;

    public SignupDuplicateChecker(SignupAccountRepository accounts) {
        this.accounts = accounts;
    }

    public List<String> findDuplicateFields(SignupRequest request) {
        List<String> duplicates = new ArrayList<>();
        if (accounts.existsByEmail(request.email())) duplicates.add("email");
        if (accounts.existsBySoDienThoai(request.soDienThoai())) duplicates.add("soDienThoai");
        if (accounts.existsByCccd(request.cccd())) duplicates.add("cccd");
        return List.copyOf(duplicates);
    }
}
