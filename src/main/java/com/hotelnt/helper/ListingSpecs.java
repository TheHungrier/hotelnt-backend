package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** Xay dieu kien WHERE dong cho bo loc tim kiem. */
public final class ListingSpecs {
    private ListingSpecs() {}

    /**
     * Chi lay co so APPROVED va chu co so ACTIVE. Cac tieu chi khac ket hop bang AND;
     * maPhuong, maLoai la OR trong cung nhom; tien ich phai khop tat ca.
     *
     * @param bounds          khung ban do (chi map-markers), null neu khong dung
     * @param needCoordinates true neu bat buoc co toa do (map-markers)
     */
    public static Specification<CoSoLuuTru> forGuests(ListingFilter f, MapBounds bounds, boolean needCoordinates) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Join<CoSoLuuTru, TaiKhoan> host = root.join("host");
            p.add(cb.equal(root.get("trangThai"), ListingStatus.APPROVED));
            p.add(cb.equal(host.get("trangThai"), AccountStatus.ACTIVE));

            if (needCoordinates) {
                p.add(cb.isNotNull(root.get("latitude")));
                p.add(cb.isNotNull(root.get("longitude")));
            }
            if (f.keyword() != null && !f.keyword().isBlank()) {
                String like = "%" + f.keyword().trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("tenCSLT")), like),
                        cb.like(cb.lower(root.<String>get("diaChiChiTiet")), like)));
            }
            if (f.giaMin() != null) {
                p.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("gia"), BigDecimal.valueOf(f.giaMin())));
            }
            if (f.giaMax() != null) {
                p.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("gia"), BigDecimal.valueOf(f.giaMax())));
            }
            if (notEmpty(f.maPhuong())) p.add(root.get("phuong").get("maPhuong").in(f.maPhuong()));
            if (notEmpty(f.maLoai())) p.add(root.get("loai").get("maLoai").in(f.maLoai()));

            List<Integer> amenityList = f.allAmenityIds();
            if (!amenityList.isEmpty()) {
                Set<Integer> ids = new HashSet<>(amenityList);
                Subquery<Long> sq = query.subquery(Long.class);
                Root<CoSoLuuTru> sr = sq.from(CoSoLuuTru.class);
                Join<CoSoLuuTru, TienIch> sj = sr.join("tienIch");
                sq.select(cb.countDistinct(sj.get("maTienIch")))
                        .where(cb.equal(sr.get("maCSLT"), root.get("maCSLT")),
                                sj.get("maTienIch").in(ids));
                p.add(cb.equal(sq, Long.valueOf(ids.size())));
            }
            if (bounds != null) {
                p.add(cb.between(root.<BigDecimal>get("latitude"),
                        BigDecimal.valueOf(bounds.swLat()), BigDecimal.valueOf(bounds.neLat())));
                p.add(cb.between(root.<BigDecimal>get("longitude"),
                        BigDecimal.valueOf(bounds.swLng()), BigDecimal.valueOf(bounds.neLng())));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static boolean notEmpty(List<?> l) {
        return l != null && !l.isEmpty();
    }
}
