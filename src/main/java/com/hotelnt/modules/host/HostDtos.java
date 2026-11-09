package com.hotelnt.modules.host;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class HostDtos {
    private HostDtos() {}
    public record Profile(Integer id, String vaiTro, String trangThai, String hoTen, String soDienThoai,
                          String email, String cccd, Instant ngayTao) {}
    public record ProfileUpdate(@Size(min=1,max=100) String hoTen,
                                @Pattern(regexp="^0\\d{9}$") String soDienThoai) {}
    public record ProfileUpdated(Integer id,String hoTen,String soDienThoai,String email,Instant ngayCapNhat) {}
    public record PasswordChange(@NotBlank String oldPassword, @NotBlank @Size(min=8,max=72)
        @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).+$") String newPassword) {}
    public record Contact(@Pattern(regexp="^0\\d{9}$") String soDienThoai, @Size(max=255) String zalo,
                          @Size(max=255) String facebook) {}
    public record ListingWrite(@NotBlank @Size(max=150) String tenCSLT, @NotNull @Positive Integer maLoai,
        @NotNull @Positive Integer maPhuong, @NotBlank @Size(max=200) String diaChiChiTiet,
        @Size(max=500) String moTa, @Valid Contact lienHe,
        @DecimalMin("500000") @DecimalMax("100000000") @Digits(integer=9,fraction=0) BigDecimal gia,
        List<@NotNull @Positive Integer> maTienIch, BigDecimal latitude, BigDecimal longitude) {}
    public record ListingQuery(List<String> trangThai,@Size(max=100) String keyword,
        @Min(1) Integer page,@Min(1) @Max(50) Integer limit) {}
    public record Pagination(long totalItems,int totalPages,int currentPage,int limit) {}
    public record ListingPage(List<ListingSummary> items,Pagination pagination) {}
    public record ListingSummary(Integer maCSLT,String tenCSLT,String tenLoai,BigDecimal gia,String duongDanAnh,
        String trangThai,String lyDoTuChoi,String lyDoViPham,Instant ngayTao,Instant ngayCapNhat,List<String> allowedActions) {}
    public record MediaInfo(Integer mediaId,String url,String type,Integer kichThuoc,Integer thuTu) {}
    public record Checklist(boolean ten,boolean diaChi,boolean phuong,boolean toaDo,boolean gia,
        boolean anhToiThieu3,boolean tienIchToiThieu1) {}
    public record ListingDetail(Integer maCSLT,String tenCSLT,Integer maLoai,String tenLoai,Integer maPhuong,
        String tenPhuong,String diaChiChiTiet,String moTa,BigDecimal latitude,BigDecimal longitude,BigDecimal gia,
        Instant ngayCapNhatGia,Contact lienHe,List<Integer> maTienIch,List<MediaInfo> media,long soAnh,long soVideo,
        String trangThai,String lyDoTuChoi,String lyDoViPham,Instant ngayTao,Instant ngayCapNhat,
        Checklist checklistGuiDuyet,List<String> allowedActions) {}
    public record Created(Integer maCSLT,String trangThai,Instant ngayTao) {}
    public record Updated(Integer maCSLT,String trangThai,String trangThaiTruoc,Instant ngayCapNhat,
        Instant ngayCapNhatGia,boolean thayDoiTrangThai,String message) {}
    public record Submitted(Integer maCSLT,String trangThai,Instant ngayGuiDuyet) {}
    public record Withdrawn(Integer maCSLT,String trangThai) {}
    public record Message(String message) {}
    /** Media module consumes after commit to remove storage files. DB media remains for retryable cleanup. */
    public record ListingDeleted(Integer maCSLT,List<MediaInfo> media) {}
}
