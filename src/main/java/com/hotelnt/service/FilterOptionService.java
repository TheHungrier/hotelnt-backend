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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.hotelnt.dto.FilterOptionDtos.*;

@Service
@Transactional(readOnly = true)
public class FilterOptionService {
    // Gia tri co dinh theo API_DESIGN.xlsx (GET /filter-options)
    private static final PriceRange PRICE_RANGE = new PriceRange(500_000L, 100_000_000L);
    private static final List<Integer> RADIUS_OPTIONS = List.of(1, 2, 3, 5, 10);
    private static final List<SortOption> SORT_OPTIONS = List.of(
            new SortOption("GIA_ASC", "Giá tăng dần"),
            new SortOption("GIA_DESC", "Giá giảm dần"),
            new SortOption("KHOANG_CACH_ASC", "Khoảng cách gần nhất"));
    // Khung ban do khu vuc Nha Trang
    private static final MapBoundsRange MAP_BOUNDS = new MapBoundsRange(12.1, 12.45, 109.1, 109.3);

    private final PhuongRepository phuongRepo;
    private final LoaiCoSoRepository loaiRepo;
    private final TienIchRepository tienIchRepo;

    public FilterOptionService(PhuongRepository phuongRepo, LoaiCoSoRepository loaiRepo, TienIchRepository tienIchRepo) {
        this.phuongRepo = phuongRepo;
        this.loaiRepo = loaiRepo;
        this.tienIchRepo = tienIchRepo;
    }

    public FilterOptionsResponse get() {
        List<Ward> wards = phuongRepo.findAllByOrderByMaPhuongAsc().stream()
                .map(p -> new Ward(p.getMaPhuong(), p.getTenPhuong())).toList();
        List<PropertyType> types = loaiRepo.findAllByOrderByMaLoaiAsc().stream()
                .map(l -> new PropertyType(l.getMaLoai(), l.getTenLoai())).toList();
        return new FilterOptionsResponse(wards, types, options("TIEN_NGHI"), options("LAN_CAN"),
                PRICE_RANGE, RADIUS_OPTIONS, SORT_OPTIONS, MAP_BOUNDS);
    }

    private List<Option> options(String nhom) {
        return tienIchRepo.findByNhomOrderByMaTienIchAsc(nhom).stream()
                .map((TienIch t) -> new Option(t.getMaTienIch(), t.getTenTienIch(), t.getIcon())).toList();
    }
}
