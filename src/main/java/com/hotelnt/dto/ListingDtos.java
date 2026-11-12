package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import java.time.Instant;
import java.util.List;

/** Hinh dang JSON dung theo cot Output trong API_DESIGN.xlsx. */
public final class ListingDtos {
    private ListingDtos() {}

    public record Amenity(int maTienIch, String tenTienIch, String icon) {}
    public record AmenityWithGroup(int maTienIch, String tenTienIch, String icon, String nhom) {}
    public record Media(int mediaId, String url, int thuTu) {}
    public record Owner(String hoTen) {}
    public record Contact(String soDienThoai, String zalo, String facebook) {}
    public record Pagination(int totalItems, int totalPages, int currentPage, int limit) {}

    /** Dung chung cho danh sach (/listings) va preview (/listings/{id}/preview). */
    public record ListingCard(int maCSLT, String tenCSLT, String tenLoai, String tenPhuong, String diaChiChiTiet,
                              Long gia, Instant ngayCapNhatGia, Double latitude, Double longitude,
                              String duongDanAnh, List<Amenity> tienIchNoiBat, Double khoangCach) {}

    public record ListingPage(List<ListingCard> items, Pagination pagination) {}

    public record Marker(int maCSLT, String tenCSLT, String tenLoai, Long gia, Double latitude, Double longitude,
                         String duongDanAnh) {}

    public record MarkerResponse(List<Marker> markers, int totalMarkers, boolean truncated) {}

    public record ListingDetail(int maCSLT, String tenCSLT, String tenLoai, int maPhuong, String tenPhuong,
                                String diaChiChiTiet, String moTa, Double latitude, Double longitude, Long gia,
                                Instant ngayCapNhatGia, Instant ngayCapNhat, Double khoangCach, Owner chuCoSo,
                                Contact lienHe, List<Media> hinhAnh, List<Media> video,
                                List<AmenityWithGroup> tienIch) {}
}
