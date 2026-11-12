package com.hotelnt.service;


import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelnt.dto.SeoDtos.SeoResponse;

@Service
@Transactional(readOnly = true)
public class SeoService {
    // Khoa trong bang CauHinh (khop db/seed.sql)
    private static final String PREFIX = "seo.";

    private final CauHinhRepository repo;

    public SeoService(CauHinhRepository repo) {
        this.repo = repo;
    }

    public SeoResponse get() {
        Map<String, String> v = repo.findAll().stream()
                .filter(c -> c.getKhoa() != null && c.getKhoa().startsWith(PREFIX))
                .collect(Collectors.toMap(CauHinh::getKhoa, c -> c.getGiaTri() == null ? "" : c.getGiaTri(),
                        (a, b) -> a));
        return new SeoResponse(v.getOrDefault("seo.metaTitle", ""), v.getOrDefault("seo.metaDescription", ""),
                v.getOrDefault("seo.slugUrl", ""), v.getOrDefault("seo.ogTitle", ""),
                v.getOrDefault("seo.ogImageUrl", ""));
    }
}
