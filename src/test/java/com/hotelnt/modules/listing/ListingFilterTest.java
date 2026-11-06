package com.hotelnt.modules.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Test logic thuan, khong can Spring hay SQL Server. */
class ListingFilterTest {

    private static ListingFilter filter(Long giaMin, Long giaMax, List<Integer> tienIch, List<Integer> lanCan,
                                        Double lat, Double lng, Double radiusKm) {
        return new ListingFilter(null, giaMin, giaMax, null, null, tienIch, lanCan, lat, lng, radiusKm);
    }

    @Test void allAmenityIdsMergesBothGroups() {
        ListingFilter f = filter(null, null, List.of(1, 2), List.of(3), null, null, null);
        assertEquals(List.of(1, 2, 3), f.allAmenityIds());
    }

    @Test void allAmenityIdsEmptyWhenNothingSelected() {
        assertTrue(filter(null, null, null, null, null, null, null).allAmenityIds().isEmpty());
    }

    @Test void minPriceGreaterThanMaxIsError() {
        var errors = filter(5L, 1L, null, null, null, null, null).crossFieldErrors();
        assertEquals(1, errors.size());
        assertEquals("giaMin", errors.get(0).get("field"));
    }

    @Test void radiusWithoutPositionIsError() {
        var errors = filter(null, null, null, null, null, null, 3.0).crossFieldErrors();
        assertEquals(1, errors.size());
        assertEquals("radiusKm", errors.get(0).get("field"));
    }

    @Test void radiusWithPositionIsOk() {
        assertTrue(filter(null, null, null, null, 12.24, 109.19, 3.0).crossFieldErrors().isEmpty());
    }

    @Test void validPriceRangeIsOk() {
        assertTrue(filter(1L, 5L, null, null, null, null, null).crossFieldErrors().isEmpty());
    }
}
