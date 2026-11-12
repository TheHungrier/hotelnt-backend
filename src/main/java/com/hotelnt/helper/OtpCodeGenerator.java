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
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Tao ma OTP gom sau chu so, giu ca cac so 0 o dau. */
@Component
public class OtpCodeGenerator {
    private final SecureRandom random;

    public OtpCodeGenerator() {
        this(new SecureRandom());
    }

    public OtpCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
}
