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
}
