package com.hotelnt.modules.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
}
