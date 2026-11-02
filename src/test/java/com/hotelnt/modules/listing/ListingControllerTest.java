package com.hotelnt.modules.listing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.modules.listing.ListingDtos.ListingPage;
import com.hotelnt.modules.listing.ListingDtos.Pagination;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Test tang web (khong can SQL Server): service duoc gia lap. */
@WebMvcTest(ListingController.class)
class ListingControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ListingService service;

    @Test void listOk() throws Exception {
        when(service.search(any())).thenReturn(new ListingPage(List.of(), new Pagination(0, 0, 1, 5)));
        mvc.perform(get("/listings?maPhuong=1&maPhuong=2&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit").value(5));
    }

    @Test void detailNotFound() throws Exception {
        when(service.detail(eq(999), any(), any())).thenThrow(new AppException(ErrorCode.LISTING_NOT_FOUND));
        mvc.perform(get("/listings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));
    }

    @Test void priceRangeInvalid() throws Exception {
        mvc.perform(get("/listings?giaMin=5&giaMax=1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void limitTooBig() throws Exception {
        mvc.perform(get("/listings?limit=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void sortByDistanceNeedsPosition() throws Exception {
        mvc.perform(get("/listings?sortBy=KHOANG_CACH_ASC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void idNotNumber() throws Exception {
        mvc.perform(get("/listings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void mapMarkersMissingBounds() throws Exception {
        mvc.perform(get("/listings/map-markers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
