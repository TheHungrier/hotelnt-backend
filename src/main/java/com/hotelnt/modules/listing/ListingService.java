package com.hotelnt.modules.listing;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.modules.listing.ListingDtos.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ListingService {

    public ListingPage search(ListingSearchRequest q) {
        validateCrossFields(q);
        // TODO: truy van DB: chi lay CSLT trangThai = APPROVED va Host trangThai = ACTIVE;
        //       maPhuong/maLoai: OR; maTienIch/maTienIchLanCan: phai khop TAT CA; sort + phan trang
        List<ListingCard> items = MockData.CARDS;
        int limit = q.limitOrDefault();
        int totalPages = (int) Math.ceil(items.size() / (double) limit);
        return new ListingPage(items, new Pagination(items.size(), totalPages, q.pageOrDefault(), limit));
    }

    public MarkerResponse mapMarkers() {
        // TODO: loc theo bounds + bo loc dung chung voi search(); toi da 500 marker
        return new MarkerResponse(MockData.MARKERS, MockData.MARKERS.size(), false);
    }

    public ListingCard preview(int maCSLT) {
        return MockData.CARDS.stream().filter(c -> c.maCSLT() == maCSLT).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.LISTING_NOT_FOUND));
    }

    public ListingDetail detail(int maCSLT) {
        return MockData.DETAILS.stream().filter(d -> d.maCSLT() == maCSLT).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.LISTING_NOT_FOUND));
    }

    /** Cac rang buoc cheo giua nhieu truong (theo cot Errors cua spec). */
    private void validateCrossFields(ListingSearchRequest q) {
        List<Map<String, String>> errors = new ArrayList<>();
        if (q.giaMin() != null && q.giaMax() != null && q.giaMin() > q.giaMax()) {
            errors.add(Map.of("field", "giaMin", "message", "giaMin không được lớn hơn giaMax"));
        }
        if (q.sortByOrDefault() == ListingSearchRequest.SortBy.KHOANG_CACH_ASC
                && (q.latitude() == null || q.longitude() == null)) {
            errors.add(Map.of("field", "sortBy", "message", "Sắp xếp theo khoảng cách cần latitude và longitude"));
        }
        if (!errors.isEmpty()) throw new AppException(ErrorCode.VALIDATION_ERROR, errors);
    }
}
