package com.hotelnt.helper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Times {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private Times() {
    }

    /**
     * Cot DATETIME khong co mui gio: coi la gio Viet Nam, tra ra Instant (ISO, ket
     * thuc bang Z).
     */
    public static Instant toInstant(LocalDateTime t) {
        return t == null ? null : t.atZone(VN).toInstant();
    }
}
