package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Bo loc noi bo dung chung cho /listings va /listings/map-markers. */
public record ListingFilter(
        String keyword, Long giaMin, Long giaMax,
        List<Integer> maPhuong, List<Integer> maLoai,
        List<Integer> maTienIch, List<Integer> maTienIchLanCan,
        Double latitude, Double longitude, Double radiusKm) {

    public boolean hasPosition() {
        return latitude != null && longitude != null;
    }

    /** Tat ca tien ich (tien nghi + lan can) ma co so phai khop DAY DU. */
    public List<Integer> allAmenityIds() {
        List<Integer> all = new ArrayList<>();
        if (maTienIch != null) all.addAll(maTienIch);
        if (maTienIchLanCan != null) all.addAll(maTienIchLanCan);
        return all;
    }

    /** Rang buoc cheo giua nhieu truong (theo cot Errors cua spec). */
    public List<Map<String, String>> crossFieldErrors() {
        List<Map<String, String>> errors = new ArrayList<>();
        if (giaMin != null && giaMax != null && giaMin > giaMax) {
            errors.add(Map.of("field", "giaMin", "message", "giaMin không được lớn hơn giaMax"));
        }
        if (radiusKm != null && !hasPosition()) {
            errors.add(Map.of("field", "radiusKm", "message", "Bán kính cần latitude và longitude"));
        }
        return errors;
    }
}
