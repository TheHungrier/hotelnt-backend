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
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class ChatbotSpecs {

    private ChatbotSpecs() {}

    public static Specification<CoSoLuuTru> visibleApproved() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("loai", JoinType.LEFT);
                root.fetch("phuong", JoinType.LEFT);
            }
            return cb.and(
                    cb.equal(root.get("trangThai"), "APPROVED"),
                    cb.equal(root.join("host", JoinType.INNER).get("trangThai"), "ACTIVE")
            );
        };
    }
}
