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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Times {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private Times() {}

    /** Cot DATETIME khong co mui gio: coi la gio Viet Nam, tra ra Instant (ISO, ket thuc bang Z). */
    public static Instant toInstant(LocalDateTime t) {
        return t == null ? null : t.atZone(VN).toInstant();
    }
}
