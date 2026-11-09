package com.hotelnt.modules.admin;

import com.hotelnt.domain.AccountStatus;
import com.hotelnt.domain.CoSoLuuTru;
import com.hotelnt.domain.TaiKhoan;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AdminSpecs {
    private AdminSpecs() {}

    public static Specification<TaiKhoan> hostFilter(String trangThai, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chi lay tai khoan Host
            predicates.add(cb.equal(root.get("vaiTro"), AccountStatus.ROLE_HOST));

            if (trangThai != null && !trangThai.isBlank()) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai.trim().toUpperCase()));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate hoTenMatch = cb.like(cb.lower(root.get("hoTen")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate sdtMatch = cb.like(root.get("soDienThoai"), pattern);
                predicates.add(cb.or(hoTenMatch, emailMatch, sdtMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<CoSoLuuTru> listingFilter(String trangThai) {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("host", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("loai", jakarta.persistence.criteria.JoinType.LEFT);
            }
            if (trangThai == null || trangThai.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("trangThai"), trangThai.trim().toUpperCase());
        };
    }
}
