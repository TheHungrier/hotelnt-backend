package com.hotelnt.modules.listing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.modules.listing.ListingDtos.Amenity;
import com.hotelnt.modules.listing.ListingDtos.AmenityWithGroup;
import com.hotelnt.modules.listing.ListingDtos.Contact;
import com.hotelnt.modules.listing.ListingDtos.ListingCard;
import com.hotelnt.modules.listing.ListingDtos.ListingDetail;
import com.hotelnt.modules.listing.ListingDtos.Marker;
import com.hotelnt.modules.listing.ListingDtos.MarkerResponse;
import com.hotelnt.modules.listing.ListingDtos.Media;
import com.hotelnt.modules.listing.ListingDtos.Owner;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Kiem tra hinh dang JSON va ma loi theo API_DESIGN.xlsx. Service duoc gia lap, khong can SQL Server. */
@WebMvcTest(ListingController.class)
class ListingContractTest {

    private static final String BOUNDS = "boundsNorthEast=12.45,109.3&boundsSouthWest=12.1,109.1";
    private static final Instant NOW = Instant.parse("2026-10-10T08:00:00Z");

    @Autowired MockMvc mvc;
    @MockBean ListingService service;

    private static ListingCard card() {
        return new ListingCard(1, "Villa View Biển", "Villa", "Phước Hải", "12 Trần Phú", 1_500_000L, NOW,
                12.2451, 109.1943, "https://x/1.jpg", List.of(new Amenity(1, "Hồ bơi", "pool")), 0.75);
    }

    @Test void mapMarkersOk() throws Exception {
        when(service.mapMarkers(any(), any())).thenReturn(new MarkerResponse(
                List.of(new Marker(1, "Villa View Biển", "Villa", 1_500_000L, 12.2451, 109.1943, "https://x/1.jpg")),
                1, false));
        mvc.perform(get("/listings/map-markers?" + BOUNDS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markers[0].maCSLT").value(1))
                .andExpect(jsonPath("$.markers[0].tenLoai").value("Villa"))
                .andExpect(jsonPath("$.markers[0].duongDanAnh").value("https://x/1.jpg"))
                .andExpect(jsonPath("$.totalMarkers").value(1))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test void mapMarkersBadBoundsFormat() throws Exception {
        mvc.perform(get("/listings/map-markers?boundsNorthEast=abc&boundsSouthWest=12.1,109.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void mapMarkersRadiusNeedsPosition() throws Exception {
        mvc.perform(get("/listings/map-markers?" + BOUNDS + "&radiusKm=2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void previewOk() throws Exception {
        when(service.preview(eq(1), any(), any())).thenReturn(card());
        mvc.perform(get("/listings/1/preview?latitude=12.24&longitude=109.19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maCSLT").value(1))
                .andExpect(jsonPath("$.tenPhuong").value("Phước Hải"))
                .andExpect(jsonPath("$.gia").value(1500000))
                .andExpect(jsonPath("$.tienIchNoiBat[0].icon").value("pool"))
                .andExpect(jsonPath("$.khoangCach").value(0.75));
    }

    @Test void previewNotFound() throws Exception {
        when(service.preview(eq(999), any(), any())).thenThrow(new AppException(ErrorCode.LISTING_NOT_FOUND));
        mvc.perform(get("/listings/999/preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));
    }

    @Test void previewIdNotNumber() throws Exception {
        mvc.perform(get("/listings/abc/preview"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void detailOkHidesHostPrivateData() throws Exception {
        when(service.detail(eq(1), any(), any())).thenReturn(new ListingDetail(
                1, "Villa View Biển", "Villa", 1, "Phước Hải", "12 Trần Phú", "Mô tả", 12.2451, 109.1943,
                1_500_000L, NOW, NOW, null, new Owner("Nguyễn Văn A"),
                new Contact("0901234567", null, null),
                List.of(new Media(10, "https://x/1.jpg", 1)), List.of(),
                List.of(new AmenityWithGroup(1, "Hồ bơi", "pool", "TIEN_NGHI"))));
        mvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chuCoSo.hoTen").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.chuCoSo.email").doesNotExist())
                .andExpect(jsonPath("$.chuCoSo.cccd").doesNotExist())
                .andExpect(jsonPath("$.lienHe.soDienThoai").value("0901234567"))
                .andExpect(jsonPath("$.hinhAnh[0].mediaId").value(10))
                .andExpect(jsonPath("$.hinhAnh[0].url").value("https://x/1.jpg"))
                .andExpect(jsonPath("$.video").isEmpty())
                .andExpect(jsonPath("$.tienIch[0].nhom").value("TIEN_NGHI"));
    }
}
