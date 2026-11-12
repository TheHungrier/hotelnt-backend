package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminDtos {
    private AdminDtos() {}

    public record HostListItem(
            Integer id,
            String hoTen,
            String email,
            String soDienThoai,
            String trangThai,
            long soLuongCSLT,
            String lyDoKhoa,
            Instant ngayTao
    ) {}

    public record Pagination(
            long totalItems,
            int totalPages,
            int currentPage,
            int limit
    ) {}

    public record HostPageResponse(
            List<HostListItem> items,
            Pagination pagination
    ) {}

    public record HostDetail(
            Integer id,
            String hoTen,
            String email,
            String soDienThoai,
            String cccd,
            String trangThai,
            String lyDoKhoa,
            Instant ngayKhoa,
            Instant ngayTao,
            long soLuongCSLT,
            Map<String, Long> thongKeCSLT
    ) {}

    public record UpdateHostStatusRequest(
            @NotBlank(message = "Chỉ nhận ACTIVE hoặc LOCKED")
            @Pattern(regexp = "^(ACTIVE|LOCKED)$", message = "Chỉ nhận ACTIVE hoặc LOCKED")
            String trangThai,
            @Size(max = 500, message = "Lý do khóa không quá 500 ký tự")
            String lyDoKhoa
    ) {}

    public record UpdateHostStatusResponse(
            Integer id,
            String trangThai,
            String lyDoKhoa,
            Instant ngayCapNhat
    ) {}

    // ------------------------------------------------------------- FR-21 Moderation Review
    public record FieldError(
            String field,
            String message
    ) {}

    public record ListingReviewListItem(
            Integer maCSLT,
            String tenCSLT,
            String tenLoai,
            String duongDanAnh,
            String hoTen,
            String soDienThoaiHost,
            String trangThai,
            Instant ngayTao,
            Instant ngayGuiDuyet,
            Instant ngayCapNhat
    ) {}

    public record ListingReviewPageResponse(
            List<ListingReviewListItem> items,
            Pagination pagination
    ) {}

    public record ContactInfo(
            String soDienThoai,
            String zalo,
            String facebook
    ) {}

    public record AmenityItem(
            Integer maTienIch,
            String tenTienIch,
            String nhom
    ) {}

    public record MediaItem(
            Integer mediaId,
            String url,
            String type,
            Integer thuTu
    ) {}

    public record HostInfo(
            Integer id,
            String hoTen,
            String email,
            String soDienThoai,
            String trangThai
    ) {}

    public record ListingChecklist(
            boolean thongTinBatBuoc,
            boolean anhToiThieu3,
            boolean toaDoTrongNhaTrang
    ) {}

    public record ListingReviewDetail(
            Integer maCSLT,
            String tenCSLT,
            Integer maLoai,
            String tenLoai,
            String tenPhuong,
            String diaChiChiTiet,
            String moTa,
            Double latitude,
            Double longitude,
            Long gia,
            Instant ngayCapNhatGia,
            ContactInfo lienHe,
            List<AmenityItem> tienIch,
            List<MediaItem> media,
            HostInfo chuCoSo,
            String trangThai,
            String lyDoTuChoi,
            String lyDoViPham,
            Instant ngayTao,
            Instant ngayGuiDuyet,
            Instant ngayCapNhat,
            ListingChecklist checklistDuyet,
            List<String> allowedActions
    ) {}

    // ------------------------------------------------------------- FR-22 & FR-23 Moderation (Approve / Reject)
    public record ModerateListingRequest(
            @NotBlank(message = "Chỉ nhận APPROVE hoặc REJECT")
            @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Chỉ nhận APPROVE hoặc REJECT")
            String action,
            @Size(max = 500, message = "Lý do từ chối không quá 500 ký tự")
            String lyDoTuChoi
    ) {}

    public record ModerateListingResponse(
            Integer maCSLT,
            String trangThai,
            String lyDoTuChoi,
            Instant ngayCapNhat,
            Instant ngayDuyet
    ) {}

    // ------------------------------------------------------------- FR-24 Unpublish (Hide / Remove)
    public record UnpublishListingRequest(
            @NotBlank(message = "Chỉ nhận HIDE hoặc REMOVE")
            @Pattern(regexp = "^(HIDE|REMOVE)$", message = "Chỉ nhận HIDE hoặc REMOVE")
            String hinhThuc,

            @NotBlank(message = "Bắt buộc nhập lý do")
            @Size(max = 500, message = "Lý do vi phạm không quá 500 ký tự")
            String lyDoViPham
    ) {}

    public record UnpublishListingResponse(
            Integer maCSLT,
            String trangThai,
            String lyDoViPham,
            Instant ngayCapNhat
    ) {}
}
