package com.hotelnt.modules.auth;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Tim tat ca cac truong trung voi tai khoan da ton tai. */
@Service
class SignupDuplicateChecker {
    private final SignupAccountRepository accounts;

    SignupDuplicateChecker(SignupAccountRepository accounts) {
        this.accounts = accounts;
    }

    List<String> findDuplicateFields(SignupRequest request) {
        List<String> duplicates = new ArrayList<>();
        if (accounts.existsByEmail(request.email())) duplicates.add("email");
        if (accounts.existsBySoDienThoai(request.soDienThoai())) duplicates.add("soDienThoai");
        if (accounts.existsByCccd(request.cccd())) duplicates.add("cccd");
        return List.copyOf(duplicates);
    }
}
