package com.hotelnt.modules.listing;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Query cua GET /listings/map-markers: khung ban do bat buoc + cung bo loc voi /listings. */
public record MapMarkerRequest(
        @NotBlank(message = "Thiếu boundsNorthEast (lat,lng)") String boundsNorthEast,
        @NotBlank(message = "Thiếu boundsSouthWest (lat,lng)") String boundsSouthWest,
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
