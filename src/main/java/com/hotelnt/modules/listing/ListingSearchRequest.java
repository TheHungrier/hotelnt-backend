package com.hotelnt.modules.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Query cua GET /listings. Spring tu bind tu ?maPhuong=1&maPhuong=2&... */
public record ListingSearchRequest(
        @Size(max = 100, message = "Tối đa 100 ký tự") String keyword,
        Long giaMin,
        Long giaMax,
        List<Integer> maPhuong,
        List<Integer> maLoai,
        List<Integer> maTienIch,
        List<Integer> maTienIchLanCan,
        Double latitude,
        Double longitude,
        @DecimalMin(value = "0", inclusive = false, message = "Bán kính phải lớn hơn 0 km") Double radiusKm,
        SortBy sortBy,
        @Min(value = 1, message = "page tối thiểu là 1") Integer page,
        @Min(value = 1, message = "limit tối thiểu là 1") @Max(value = 50, message = "limit tối đa là 50") Integer limit) {

    public enum SortBy { GIA_ASC, GIA_DESC, KHOANG_CACH_ASC, NGAY_CAP_NHAT_DESC }

    public SortBy sortByOrDefault() { return sortBy == null ? SortBy.NGAY_CAP_NHAT_DESC : sortBy; }
    public int pageOrDefault() { return page == null ? 1 : page; }
    public int limitOrDefault() { return limit == null ? 10 : limit; }
}
