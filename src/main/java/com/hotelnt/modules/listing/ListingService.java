package com.hotelnt.modules.listing;

import com.hotelnt.common.GeoUtils;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import com.hotelnt.common.util.Times;
import com.hotelnt.domain.CoSoLuuTru;
import com.hotelnt.domain.LienHe;
import com.hotelnt.domain.Media;
import com.hotelnt.domain.TienIch;
import com.hotelnt.modules.listing.ListingDtos.*;
import com.hotelnt.repository.CoSoLuuTruRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListingService {
    private static final int MAX_MARKERS = 500;
    private static final int HIGHLIGHT_AMENITIES = 3;
    // Gia tri cot Media.loai, LienHe.loaiLienHe, TienIch.nhom (khop CHECK trong db/schema.sql)
    private static final String IMAGE = "IMAGE";
    private static final String VIDEO = "VIDEO";
    private static final String TIEN_NGHI = "TIEN_NGHI";

    private final CoSoLuuTruRepository repo;

    public ListingService(CoSoLuuTruRepository repo) {
        this.repo = repo;
    }

    // ------------------------------------------------------------------ GET /listings
    public ListingPage search(ListingSearchRequest q) {
        ListingFilter f = q.filter();
        ListingSearchRequest.SortBy sort = q.sortByOrDefault();
        int page = q.pageOrDefault();
        int limit = q.limitOrDefault();
        var spec = ListingSpecs.forGuests(f, null, false);

        boolean inMemory = sort == ListingSearchRequest.SortBy.KHOANG_CACH_ASC || f.radiusKm() != null;
        if (!inMemory) {
            // Truong hop pho bien: loc, sap xep, phan trang ngay trong DB
            Page<CoSoLuuTru> result = repo.findAll(spec, PageRequest.of(page - 1, limit, toSort(sort)));
            List<ListingCard> items = result.getContent().stream().map(c -> toCard(c, f.latitude(), f.longitude())).toList();
            return new ListingPage(items,
                    new Pagination((int) result.getTotalElements(), result.getTotalPages(), page, limit));
        }

        // Co khoang cach/ban kinh: tinh Haversine bang Java roi loc, sap xep, phan trang trong bo nho.
        // TODO (toi uu neu du lieu lon): chuyen sang truy van SQL dung ACOS/COS/SIN.
        List<ListingCard> all = new ArrayList<>();
        for (CoSoLuuTru c : repo.findAll(spec, toSort(sort))) {
            ListingCard card = toCard(c, f.latitude(), f.longitude());
            if (f.radiusKm() != null && (card.khoangCach() == null || card.khoangCach() > f.radiusKm())) continue;
            all.add(card);
        }
        if (sort == ListingSearchRequest.SortBy.KHOANG_CACH_ASC) {
            all.sort(Comparator.comparing(ListingCard::khoangCach, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        int total = all.size();
        int from = Math.min((page - 1) * limit, total);
        int to = Math.min(from + limit, total);
        return new ListingPage(new ArrayList<>(all.subList(from, to)),
                new Pagination(total, (int) Math.ceil(total / (double) limit), page, limit));
    }

    // ------------------------------------------------------- GET /listings/map-markers
    public MarkerResponse mapMarkers(MapMarkerRequest q, MapBounds bounds) {
        ListingFilter f = q.filter();
        var spec = ListingSpecs.forGuests(f, bounds, true);
        Sort byId = Sort.by("maCSLT");

        List<CoSoLuuTru> rows;
        if (f.radiusKm() != null) {
            rows = new ArrayList<>(repo.findAll(spec, byId));
            rows.removeIf(c -> distance(c, f.latitude(), f.longitude()) > f.radiusKm());
        } else {
            // lay du 501 dong de biet co vuot gioi han 500 hay khong
            rows = repo.findAll(spec, PageRequest.of(0, MAX_MARKERS + 1, byId)).getContent();
        }
        boolean truncated = rows.size() > MAX_MARKERS;
        if (truncated) rows = rows.subList(0, MAX_MARKERS);

        List<Marker> markers = rows.stream().map(c -> new Marker(
                c.getMaCSLT(), c.getTenCSLT(), c.getLoai().getTenLoai(), toLong(c.getGia()),
                toDouble(c.getLatitude()), toDouble(c.getLongitude()), firstImage(c))).toList();
        return new MarkerResponse(markers, markers.size(), truncated);
    }

    // --------------------------------------------- GET /listings/{maCSLT}/preview, detail
    public ListingCard preview(int maCSLT, Double lat, Double lng) {
        return toCard(findVisible(maCSLT), lat, lng);
    }

    public ListingDetail detail(int maCSLT, Double lat, Double lng) {
        CoSoLuuTru c = findVisible(maCSLT);

        List<Media> images = c.getMedia().stream().filter(m -> IMAGE.equals(m.getLoai())).toList();
        List<Media> videos = c.getMedia().stream().filter(m -> VIDEO.equals(m.getLoai())).toList();

        String sdt = null, zalo = null, facebook = null;   // Host khong nhap thi de null de FE an nut
        for (LienHe l : c.getLienHe()) {
            if (l.getLoaiLienHe() == null) continue;
            switch (l.getLoaiLienHe()) {
                case "SO_DIEN_THOAI" -> sdt = l.getGiaTri();
                case "ZALO" -> zalo = l.getGiaTri();
                case "FACEBOOK" -> facebook = l.getGiaTri();
                default -> { }
            }
        }
        List<AmenityWithGroup> amenities = c.getTienIch().stream()
                .sorted(Comparator.comparing(TienIch::getMaTienIch))
                .map(t -> new AmenityWithGroup(t.getMaTienIch(), t.getTenTienIch(), t.getIcon(), t.getNhom()))
                .toList();

        return new ListingDetail(c.getMaCSLT(), c.getTenCSLT(), c.getLoai().getTenLoai(),
                c.getPhuong().getMaPhuong(), c.getPhuong().getTenPhuong(), c.getDiaChiChiTiet(), c.getMoTa(),
                toDouble(c.getLatitude()), toDouble(c.getLongitude()), toLong(c.getGia()),
                Times.toInstant(c.getNgayCapNhatGia()), Times.toInstant(c.getNgayCapNhat()),
                distanceOrNull(c, lat, lng),
                new Owner(c.getHost().getHoTen()),          // khong tra email, CCCD, id cua Host
                new Contact(sdt, zalo, facebook),
                images.stream().map(m -> new ListingDtos.Media(m.getMediaId(), m.getDuongDan(), m.getThuTu())).toList(),
                videos.stream().map(m -> new ListingDtos.Media(m.getMediaId(), m.getDuongDan(), m.getThuTu())).toList(),
                amenities);
    }

    // ------------------------------------------------------------------ helpers
    private CoSoLuuTru findVisible(int maCSLT) {
        return repo.findVisibleById(maCSLT).orElseThrow(() -> new AppException(ErrorCode.LISTING_NOT_FOUND));
    }

    private ListingCard toCard(CoSoLuuTru c, Double lat, Double lng) {
        List<Amenity> highlights = c.getTienIch().stream()
                .filter(t -> TIEN_NGHI.equals(t.getNhom()))
                .sorted(Comparator.comparing(TienIch::getMaTienIch))
                .limit(HIGHLIGHT_AMENITIES)
                .map(t -> new Amenity(t.getMaTienIch(), t.getTenTienIch(), t.getIcon()))
                .toList();
        return new ListingCard(c.getMaCSLT(), c.getTenCSLT(), c.getLoai().getTenLoai(), c.getPhuong().getTenPhuong(),
                c.getDiaChiChiTiet(), toLong(c.getGia()), Times.toInstant(c.getNgayCapNhatGia()),
                toDouble(c.getLatitude()), toDouble(c.getLongitude()),
                firstImage(c), highlights, distanceOrNull(c, lat, lng));
    }

    /** Khoang cach km (lam tron 2 chu so); null neu thieu toa do khach hoac toa do co so. */
    private static Double distanceOrNull(CoSoLuuTru c, Double lat, Double lng) {
        if (lat == null || lng == null || c.getLatitude() == null || c.getLongitude() == null) return null;
        double km = GeoUtils.distanceKm(lat, lng, c.getLatitude().doubleValue(), c.getLongitude().doubleValue());
        return Math.round(km * 100.0) / 100.0;
    }

    /** Dung khi chac chan co toa do (map-markers da loc isNotNull). */
    private static double distance(CoSoLuuTru c, Double lat, Double lng) {
        Double d = distanceOrNull(c, lat, lng);
        return d == null ? Double.MAX_VALUE : d;
    }

    private static String firstImage(CoSoLuuTru c) {
        return c.getMedia().stream().filter(m -> IMAGE.equals(m.getLoai()))
                .map(Media::getDuongDan).findFirst().orElse(null);
    }

    private static Long toLong(BigDecimal v) {
        return v == null ? null : v.longValue();
    }

    private static Double toDouble(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static Sort toSort(ListingSearchRequest.SortBy s) {
        Sort byId = Sort.by("maCSLT");
        return switch (s) {
            case GIA_ASC -> Sort.by(Sort.Direction.ASC, "gia").and(byId);
            case GIA_DESC -> Sort.by(Sort.Direction.DESC, "gia").and(byId);
            case NGAY_CAP_NHAT_DESC -> Sort.by(Sort.Direction.DESC, "ngayCapNhat").and(byId);
            case KHOANG_CACH_ASC -> byId;     // se sap xep lai theo khoang cach trong bo nho
        };
    }
}
