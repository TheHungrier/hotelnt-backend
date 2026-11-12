package com.hotelnt.controller;

import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.service.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminListingController.class)
class AdminListingControllerTest {

        @Autowired
        private MockMvc mvc;

        @MockBean
        private AdminListingService service;

        @MockBean
        private AdminIdentityResolver identities;

        @BeforeEach
        void setupAuth() {
                when(identities.resolve(any()))
                                .thenReturn(new AdminIdentityResolver.Identity(1, "admin@hotelnt.local", "ADMIN"));
        }

        @Test
        void missingToken_returns401() throws Exception {
                when(identities.resolve(any())).thenThrow(new AppException(ErrorCode.TOKEN_INVALID));

                mvc.perform(get("/admin/listings"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));

                verifyNoInteractions(service);
        }

        @Test
        void wrongRole_returns403() throws Exception {
                when(identities.resolve(any())).thenThrow(new AppException(ErrorCode.FORBIDDEN_ROLE));

                mvc.perform(get("/admin/listings"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));

                verifyNoInteractions(service);
        }

        @Test
        void getListings_defaultParams_returns200() throws Exception {
                ListingReviewListItem item = new ListingReviewListItem(
                                15, "Villa Biển Xanh Nha Trang", "Villa", "https://cdn.example.com/1.jpg",
                                "Nguyễn Văn A", "0905123456", "PENDING",
                                Instant.parse("2026-10-09T03:00:00Z"),
                                Instant.parse("2026-10-11T07:45:00Z"),
                                Instant.parse("2026-10-11T07:45:00Z"));
                ListingReviewPageResponse pageResponse = new ListingReviewPageResponse(
                                List.of(item),
                                new AdminDtos.Pagination(1, 1, 1, 10));

                when(service.getListings(null, 1, 10)).thenReturn(pageResponse);

                mvc.perform(get("/admin/listings"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items[0].maCSLT").value(15))
                                .andExpect(jsonPath("$.items[0].tenCSLT").value("Villa Biển Xanh Nha Trang"))
                                .andExpect(jsonPath("$.items[0].tenLoai").value("Villa"))
                                .andExpect(jsonPath("$.items[0].hoTen").value("Nguyễn Văn A"))
                                .andExpect(jsonPath("$.items[0].trangThai").value("PENDING"))
                                .andExpect(jsonPath("$.pagination.totalItems").value(1))
                                .andExpect(jsonPath("$.pagination.currentPage").value(1))
                                .andExpect(jsonPath("$.pagination.limit").value(10));
        }

        @Test
        void getListings_withQueryParams_passedToService() throws Exception {
                when(service.getListings("APPROVED", 2, 5)).thenReturn(
                                new ListingReviewPageResponse(List.of(), new AdminDtos.Pagination(0, 0, 2, 5)));

                mvc.perform(get("/admin/listings")
                                .param("trangThai", "APPROVED")
                                .param("page", "2")
                                .param("limit", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items").isEmpty())
                                .andExpect(jsonPath("$.pagination.currentPage").value(2))
                                .andExpect(jsonPath("$.pagination.limit").value(5));

                verify(service).getListings("APPROVED", 2, 5);
        }

        @Test
        void getListingDetail_success_returns200() throws Exception {
                ListingReviewDetail detail = new ListingReviewDetail(
                                15, "Villa Biển Xanh Nha Trang", 2, "Villa", "Vĩnh Nguyên",
                                "12 Trần Phú, Vĩnh Nguyên, Nha Trang", "Villa 3 phùng ngý, view biển.",
                                12.2451, 109.1943, 1500000L,
                                Instant.parse("2026-10-10T08:15:00Z"),
                                new ContactInfo("0905123456", "https://zalo.me/0905123456",
                                                "https://facebook.com/villabienxanh"),
                                List.of(new AmenityItem(1, "Hồ bơi", "TIEN_NGHI")),
                                List.of(new MediaItem(101, "https://cdn.example.com/listings/1.jpg", "IMAGE", 0)),
                                new HostInfo(3, "Nguyễn Văn A", "host01@example.com", "0905123456", "ACTIVE"),
                                "PENDING", null, null,
                                Instant.parse("2026-10-09T03:00:00Z"),
                                Instant.parse("2026-10-11T07:45:00Z"),
                                Instant.parse("2026-10-11T07:45:00Z"),
                                new ListingChecklist(true, true, true),
                                List.of("APPROVE", "REJECT"));

                when(service.getListingDetail(15)).thenReturn(detail);

                mvc.perform(get("/admin/listings/15"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maCSLT").value(15))
                                .andExpect(jsonPath("$.tenCSLT").value("Villa Biển Xanh Nha Trang"))
                                .andExpect(jsonPath("$.tenLoai").value("Villa"))
                                .andExpect(jsonPath("$.tenPhuong").value("Vĩnh Nguyên"))
                                .andExpect(jsonPath("$.gia").value(1500000))
                                .andExpect(jsonPath("$.checklistDuyet.thongTinBatBuoc").value(true))
                                .andExpect(jsonPath("$.checklistDuyet.anhToiThieu3").value(true))
                                .andExpect(jsonPath("$.checklistDuyet.toaDoTrongNhaTrang").value(true))
                                .andExpect(jsonPath("$.allowedActions[0]").value("APPROVE"))
                                .andExpect(jsonPath("$.allowedActions[1]").value("REJECT"));
        }

        @Test
        void getListingDetail_notFound_returns404() throws Exception {
                when(service.getListingDetail(999)).thenThrow(new AppException(ErrorCode.LISTING_NOT_FOUND));

                mvc.perform(get("/admin/listings/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));
        }

        // ------------------------------------------------------------- FR-22 & FR-23
        // Moderate Controller Tests
        @Test
        void moderateListing_approve_returns200() throws Exception {
                ModerateListingResponse res = new ModerateListingResponse(
                                15, "APPROVED", null,
                                Instant.parse("2026-10-11T07:45:00Z"),
                                Instant.parse("2026-10-11T07:45:00Z"));
                when(service.moderateListing(eq(15), any())).thenReturn(res);

                mvc.perform(patch("/admin/listings/15/moderation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\": \"APPROVE\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maCSLT").value(15))
                                .andExpect(jsonPath("$.trangThai").value("APPROVED"))
                                .andExpect(jsonPath("$.lyDoTuChoi").doesNotExist())
                                .andExpect(jsonPath("$.ngayDuyet").value("2026-10-11T07:45:00Z"));
        }

        @Test
        void moderateListing_reject_returns200() throws Exception {
                ModerateListingResponse res = new ModerateListingResponse(
                                15, "REJECTED", "Ảnh mờ",
                                Instant.parse("2026-10-11T07:45:00Z"),
                                null);
                when(service.moderateListing(eq(15), any())).thenReturn(res);

                mvc.perform(patch("/admin/listings/15/moderation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\": \"REJECT\", \"lyDoTuChoi\": \"Ảnh mờ\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maCSLT").value(15))
                                .andExpect(jsonPath("$.trangThai").value("REJECTED"))
                                .andExpect(jsonPath("$.lyDoTuChoi").value("Ảnh mờ"));
        }

        @Test
        void moderateListing_invalidAction_returns400() throws Exception {
                mvc.perform(patch("/admin/listings/15/moderation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\": \"INVALID_ACTION\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        // ------------------------------------------------------------- FR-24 Unpublish
        // Controller Tests
        @Test
        void unpublishListing_hide_returns200() throws Exception {
                UnpublishListingResponse res = new UnpublishListingResponse(
                                15, "HIDDEN", "Thông tin sai",
                                Instant.parse("2026-10-11T07:50:00Z"));
                when(service.unpublishListing(eq(15), any())).thenReturn(res);

                mvc.perform(patch("/admin/listings/15/unpublish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"hinhThuc\": \"HIDE\", \"lyDoViPham\": \"Thông tin sai\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maCSLT").value(15))
                                .andExpect(jsonPath("$.trangThai").value("HIDDEN"))
                                .andExpect(jsonPath("$.lyDoViPham").value("Thông tin sai"));
        }

        @Test
        void unpublishListing_remove_returns200() throws Exception {
                UnpublishListingResponse res = new UnpublishListingResponse(
                                15, "REMOVED", "Phản cấm",
                                Instant.parse("2026-10-11T07:50:00Z"));
                when(service.unpublishListing(eq(15), any())).thenReturn(res);

                mvc.perform(patch("/admin/listings/15/unpublish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"hinhThuc\": \"REMOVE\", \"lyDoViPham\": \"Phản cấm\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maCSLT").value(15))
                                .andExpect(jsonPath("$.trangThai").value("REMOVED"))
                                .andExpect(jsonPath("$.lyDoViPham").value("Phản cấm"));
        }

        @Test
        void unpublishListing_missingReason_returns400() throws Exception {
                mvc.perform(patch("/admin/listings/15/unpublish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"hinhThuc\": \"HIDE\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        void unpublishListing_invalidHinhThuc_returns400() throws Exception {
                mvc.perform(patch("/admin/listings/15/unpublish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"hinhThuc\": \"INVALID\", \"lyDoViPham\": \"Lý do\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        void moderateListing_conflict_returns409() throws Exception {
                when(service.moderateListing(eq(15), any()))
                                .thenThrow(new AppException(ErrorCode.INVALID_STATE_TRANSITION));

                mvc.perform(patch("/admin/listings/15/moderation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\": \"APPROVE\"}"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        }

        @Test
        void moderateListing_checklistIncomplete_returns422() throws Exception {
                when(service.moderateListing(eq(15), any()))
                                .thenThrow(new AppException(ErrorCode.CHECKLIST_INCOMPLETE,
                                                List.of(new FieldError("hinhAnh", "Cần tối thiểu 3 ảnh hợp lệ"))));

                mvc.perform(patch("/admin/listings/15/moderation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\": \"APPROVE\"}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error.code").value("CHECKLIST_INCOMPLETE"))
                                .andExpect(jsonPath("$.error.details[0].field").value("hinhAnh"));
        }
}