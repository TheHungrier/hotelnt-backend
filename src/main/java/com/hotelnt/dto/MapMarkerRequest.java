package com.hotelnt.dto;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Query cua GET /listings/map-markers: khung ban do tuy chon + cung bo loc voi /listings. */
public record MapMarkerRequest(
        String boundsNorthEast,
        String boundsSouthWest,
        @Size(max = 100, message = "Tối đa 100 ký tự") String keyword,
        Long giaMin,
        Long giaMax,
        List<Integer> maPhuong,
        List<Integer> maLoai,
        List<Integer> maTienIch,
        List<Integer> maTienIchLanCan,
        Double latitude,
        Double longitude,
        @DecimalMin(value = "0", inclusive = false, message = "Bán kính phải lớn hơn 0 km") Double radiusKm) {

    public ListingFilter filter() {
        return new ListingFilter(keyword, giaMin, giaMax, maPhuong, maLoai, maTienIch, maTienIchLanCan,
                latitude, longitude, radiusKm);
    }

    public void validateCrossFields() {
        var errors = filter().crossFieldErrors();
        if (!errors.isEmpty()) throw new AppException(ErrorCode.VALIDATION_ERROR, errors);
    }
}
