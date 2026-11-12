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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Bam va kiem tra mat khau bang bcrypt. */
@Component
public class PasswordHasher {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public String hash(String password) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Missing password");
        return bcrypt.encode(password);
    }

    public boolean matches(String password, String hash) {
        return password != null && hash != null && bcrypt.matches(password, hash);
    }
}
