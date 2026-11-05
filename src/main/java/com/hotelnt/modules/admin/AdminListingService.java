package com.hotelnt.modules.admin;

import com.hotelnt.common.error.ApiErrorResponse;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.common.util.Times;
import com.hotelnt.domain.*;
import com.hotelnt.modules.admin.AdminDtos.*;
import com.hotelnt.repository.CoSoLuuTruRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminListingService {

    public static final double NHA_TRANG_MIN_LAT = 12.10;
    public static final double NHA_TRANG_MAX_LAT = 12.45;
    public static final double NHA_TRANG_MIN_LNG = 109.10;
    public static final double NHA_TRANG_MAX_LNG = 109.30;

    private static final Set<String> VALID_STATUSES = Set.of(
            ListingStatus.PENDING,
            ListingStatus.APPROVED,
            ListingStatus.REJECTED,
            ListingStatus.HIDDEN,
            ListingStatus.REMOVED
    );

    private final CoSoLuuTruRepository repo;

    public AdminListingService(CoSoLuuTruRepository repo) {
        this.repo = repo;
    }

    // ------------------------------------------------------------------ GET /admin/listings (FR-21)
    public ListingReviewPageResponse getListings(String trangThai, int page, int limit) {
        String status = (trangThai == null || trangThai.isBlank()) ? ListingStatus.PENDING : trangThai.trim().toUpperCase();

        if (ListingStatus.DRAFT.equalsIgnoreCase(status)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, List.of(
                    new FieldError("trangThai", "Không hỗ trợ DRAFT")
            ));
        }

        if (!VALID_STATUSES.contains(status)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, List.of(
                    new FieldError("trangThai", "Trạng thái không hợp lệ")
            ));
        }

        if (page < 1) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, List.of(
                    new FieldError("page", "page phải lớn hơn hoặc bằng 1")
            ));
        }

        if (limit < 1 || limit > 100) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, List.of(
                    new FieldError("limit", "limit phải từ 1 đến 100")
            ));
        }

        Sort sort;
        if (ListingStatus.PENDING.equals(status)) {
            // FIFO theo SRS FR-21: sắp xếp tăng dần theo ngày gửi duyệt (ai gửi trước duyệt trước)
            sort = Sort.by(Sort.Order.asc("ngayGuiDuyet"), Sort.Order.asc("ngayTao"), Sort.Order.asc("maCSLT"));
        } else {
            sort = Sort.by(Sort.Order.desc("ngayCapNhat"), Sort.Order.desc("maCSLT"));
        }

        Page<CoSoLuuTru> result = repo.findAll(AdminSpecs.listingFilter(status), PageRequest.of(page - 1, limit, sort));

        List<ListingReviewListItem> items = result.getContent().stream()
                .map(this::toReviewListItem)
                .toList();

        return new ListingReviewPageResponse(
                items,
                new Pagination(result.getTotalElements(), result.getTotalPages(), page, limit)
        );
    }

    // ------------------------------------------------------------- GET /admin/listings/{maCSLT}
    public ListingReviewDetail getListingDetail(int maCSLT) {
        CoSoLuuTru c = repo.findById(maCSLT)
                .orElseThrow(() -> new AppException(ErrorCode.LISTING_NOT_FOUND));

        ContactInfo contact = extractContact(c);
        List<AmenityItem> amenities = extractAmenities(c);
        List<MediaItem> mediaItems = extractMedia(c);
        HostInfo hostInfo = extractHost(c);
        ListingChecklist checklist = buildChecklist(c);
        List<String> allowedActions = computeAllowedActions(c.getTrangThai());

        return new ListingReviewDetail(
                c.getMaCSLT(),
                c.getTenCSLT(),
                c.getLoai() != null ? c.getLoai().getMaLoai() : null,
                c.getLoai() != null ? c.getLoai().getTenLoai() : null,
                c.getPhuong() != null ? c.getPhuong().getTenPhuong() : null,
                c.getDiaChiChiTiet(),
                c.getMoTa(),
                toDouble(c.getLatitude()),
                toDouble(c.getLongitude()),
                toLong(c.getGia()),
                Times.toInstant(c.getNgayCapNhatGia()),
                contact,
                amenities,
                mediaItems,
                hostInfo,
                c.getTrangThai(),
                c.getLyDoTuChoi(),
                c.getLyDoViPham(),
                Times.toInstant(c.getNgayTao()),
                Times.toInstant(c.getNgayGuiDuyet()),
                Times.toInstant(c.getNgayCapNhat()),
                checklist,
                allowedActions
        );
    }

    // ------------------------------------------------------------------ Helpers
    private ListingReviewListItem toReviewListItem(CoSoLuuTru c) {
        String firstImg = firstImage(c);
        String hostName = c.getHost() != null ? c.getHost().getHoTen() : null;
        String hostPhone = c.getHost() != null ? c.getHost().getSoDienThoai() : null;
        String loaiName = c.getLoai() != null ? c.getLoai().getTenLoai() : null;

        return new ListingReviewListItem(
                c.getMaCSLT(),
                c.getTenCSLT(),
                loaiName,
                firstImg,
                hostName,
                hostPhone,
                c.getTrangThai(),
                Times.toInstant(c.getNgayTao()),
                Times.toInstant(c.getNgayGuiDuyet()),
                Times.toInstant(c.getNgayCapNhat())
        );
    }

    private String firstImage(CoSoLuuTru c) {
        if (c.getMedia() == null) return null;
        return c.getMedia().stream()
                .filter(m -> "IMAGE".equalsIgnoreCase(m.getLoai()))
                .sorted(Comparator.comparing(Media::getThuTu, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Media::getDuongDan)
                .findFirst()
                .orElse(null);
    }

    private ContactInfo extractContact(CoSoLuuTru c) {
        String sdt = null, zalo = null, facebook = null;
        if (c.getLienHe() != null) {
            for (LienHe l : c.getLienHe()) {
                if (l.getLoaiLienHe() == null) continue;
                switch (l.getLoaiLienHe()) {
                    case "SO_DIEN_THOAI" -> sdt = l.getGiaTri();
                    case "ZALO" -> zalo = l.getGiaTri();
                    case "FACEBOOK" -> facebook = l.getGiaTri();
                    default -> {}
                }
            }
        }
        return new ContactInfo(sdt, zalo, facebook);
    }

    private List<AmenityItem> extractAmenities(CoSoLuuTru c) {
        if (c.getTienIch() == null) return List.of();
        return c.getTienIch().stream()
                .sorted(Comparator.comparing(TienIch::getMaTienIch))
                .map(t -> new AmenityItem(t.getMaTienIch(), t.getTenTienIch(), t.getNhom()))
                .toList();
    }

    private List<MediaItem> extractMedia(CoSoLuuTru c) {
        if (c.getMedia() == null) return List.of();
        return c.getMedia().stream()
                .sorted(Comparator.comparing(Media::getThuTu, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(m -> new MediaItem(m.getMediaId(), m.getDuongDan(), m.getLoai(), m.getThuTu()))
                .toList();
    }

    private HostInfo extractHost(CoSoLuuTru c) {
        if (c.getHost() == null) return null;
        TaiKhoan h = c.getHost();
        return new HostInfo(h.getId(), h.getHoTen(), h.getEmail(), h.getSoDienThoai(), h.getTrangThai());
    }

    private ListingChecklist buildChecklist(CoSoLuuTru c) {
        boolean thongTinBatBuoc = c.getTenCSLT() != null && !c.getTenCSLT().isBlank()
                && c.getDiaChiChiTiet() != null && !c.getDiaChiChiTiet().isBlank()
                && c.getGia() != null && c.getGia().compareTo(BigDecimal.ZERO) > 0
                && c.getLoai() != null
                && c.getPhuong() != null;

        long imageCount = c.getMedia() == null ? 0 : c.getMedia().stream()
                .filter(m -> "IMAGE".equalsIgnoreCase(m.getLoai()))
                .count();
        boolean anhToiThieu3 = imageCount >= 3;

        boolean toaDoTrongNhaTrang = false;
        if (c.getLatitude() != null && c.getLongitude() != null) {
            double lat = c.getLatitude().doubleValue();
            double lng = c.getLongitude().doubleValue();
            toaDoTrongNhaTrang = (lat >= NHA_TRANG_MIN_LAT && lat <= NHA_TRANG_MAX_LAT
                    && lng >= NHA_TRANG_MIN_LNG && lng <= NHA_TRANG_MAX_LNG);
        }

        return new ListingChecklist(thongTinBatBuoc, anhToiThieu3, toaDoTrongNhaTrang);
    }

    private List<String> computeAllowedActions(String trangThai) {
        if (trangThai == null) return List.of();
        return switch (trangThai.toUpperCase()) {
            case ListingStatus.PENDING -> List.of("APPROVE", "REJECT");
            case ListingStatus.APPROVED -> List.of("HIDE", "REMOVE");
            case ListingStatus.HIDDEN -> List.of("REMOVE");
            default -> List.of();
        };
    }

    private Long toLong(BigDecimal val) {
        return val == null ? null : val.longValue();
    }

    private Double toDouble(BigDecimal val) {
        return val == null ? null : val.doubleValue();
    }
}
