package com.hotelnt.integration;

import com.hotelnt.domain.*;
import com.hotelnt.dto.ListingFilter;
import com.hotelnt.helper.ListingSpecs;
import com.hotelnt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CoSoLuuTruRepository coSoRepo;

    @Autowired
    private PhuongRepository phuongRepo;

    @Autowired
    private LoaiCoSoRepository loaiRepo;

    @Autowired
    private TienIchRepository tienIchRepo;

    @Autowired
    private TaiKhoanRepository taiKhoanRepo;

    @Autowired
    private TestEntityManager em;

    private TaiKhoan activeHost;
    private TaiKhoan lockedHost;
    private Phuong phuong1;
    private Phuong phuong2;
    private LoaiCoSo loaiHotel;
    private LoaiCoSo loaiVilla;
    private TienIch tienIchPool;
    private TienIch tienIchWifi;

    @BeforeEach
    void setUpTestData() {
        activeHost = taiKhoanRepo.findByEmail("host1@hotelnt.local").orElseGet(() -> {
            TaiKhoan h = new TaiKhoan();
            h.setHoTen("Active Host");
            h.setEmail("host_active_test@hotelnt.local");
            h.setMatKhau("secret");
            h.setVaiTro("HOST");
            h.setTrangThai(AccountStatus.ACTIVE);
            return taiKhoanRepo.save(h);
        });

        lockedHost = taiKhoanRepo.findByEmail("host3@hotelnt.local").orElseGet(() -> {
            TaiKhoan h = new TaiKhoan();
            h.setHoTen("Locked Host");
            h.setEmail("host_locked_test@hotelnt.local");
            h.setMatKhau("secret");
            h.setVaiTro("HOST");
            h.setTrangThai(AccountStatus.LOCKED);
            return taiKhoanRepo.save(h);
        });

        phuong1 = phuongRepo.findAll().get(0);
        phuong2 = phuongRepo.findAll().get(1);
        loaiHotel = loaiRepo.findAll().get(0);
        loaiVilla = loaiRepo.findAll().get(1);

        List<TienIch> tienIchList = tienIchRepo.findAll();
        tienIchPool = tienIchList.get(0);
        tienIchWifi = tienIchList.get(1);
    }

    private CoSoLuuTru createListing(String name, String address, BigDecimal gia,
                                     String status, TaiKhoan host,
                                     Phuong phuong, LoaiCoSo loai,
                                     List<TienIch> tienIchs, LocalDateTime ngayCapNhat) {
        CoSoLuuTru c = new CoSoLuuTru();
        c.setTenCSLT(name);
        c.setDiaChiChiTiet(address);
        c.setGia(gia);
        c.setTrangThai(status);
        c.setHost(host);
        c.setPhuong(phuong);
        c.setLoai(loai);
        c.setNgayCapNhat(ngayCapNhat != null ? ngayCapNhat : LocalDateTime.now());
        if (tienIchs != null) {
            c.setTienIch(new HashSet<>(tienIchs));
        }
        return coSoRepo.save(c);
    }

    @Test
    @DisplayName("Lọc theo khoảng giá giaMin và giaMax")
    void testFilterPriceMinMax() {
        createListing("Listing 500k", "Dia Chi A", BigDecimal.valueOf(500_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Listing 1000k", "Dia Chi B", BigDecimal.valueOf(1_000_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Listing 2000k", "Dia Chi C", BigDecimal.valueOf(2_000_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());

        ListingFilter filter = new ListingFilter(null, 800_000L, 1_500_000L, null, null, null, null, null, null, null);
        Specification<CoSoLuuTru> spec = ListingSpecs.forGuests(filter, null, false);
        List<CoSoLuuTru> result = coSoRepo.findAll(spec);

        assertTrue(result.stream().anyMatch(c -> c.getTenCSLT().equals("Listing 1000k")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Listing 500k")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Listing 2000k")));
    }

    @Test
    @DisplayName("Lọc theo loại cơ sở lưu trú (maLoai)")
    void testFilterMaLoai() {
        createListing("Hotel Unique", "Dia Chi Hotel", BigDecimal.valueOf(700_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Villa Unique", "Dia Chi Villa", BigDecimal.valueOf(1_800_000), ListingStatus.APPROVED, activeHost, phuong1, loaiVilla, List.of(), LocalDateTime.now());

        ListingFilter filter = new ListingFilter(null, null, null, null, List.of(loaiVilla.getMaLoai()), null, null, null, null, null);
        Specification<CoSoLuuTru> spec = ListingSpecs.forGuests(filter, null, false);
        List<CoSoLuuTru> result = coSoRepo.findAll(spec);

        assertTrue(result.stream().allMatch(c -> c.getLoai().getMaLoai().equals(loaiVilla.getMaLoai())));
        assertTrue(result.stream().anyMatch(c -> c.getTenCSLT().equals("Villa Unique")));
    }

    @Test
    @DisplayName("Lọc theo phường (maPhuong)")
    void testFilterMaPhuong() {
        createListing("Listing Phuong 1", "Dia Chi P1", BigDecimal.valueOf(600_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Listing Phuong 2", "Dia Chi P2", BigDecimal.valueOf(600_000), ListingStatus.APPROVED, activeHost, phuong2, loaiHotel, List.of(), LocalDateTime.now());

        ListingFilter filter = new ListingFilter(null, null, null, List.of(phuong2.getMaPhuong()), null, null, null, null, null, null);
        Specification<CoSoLuuTru> spec = ListingSpecs.forGuests(filter, null, false);
        List<CoSoLuuTru> result = coSoRepo.findAll(spec);

        assertTrue(result.stream().allMatch(c -> c.getPhuong().getMaPhuong().equals(phuong2.getMaPhuong())));
        assertTrue(result.stream().anyMatch(c -> c.getTenCSLT().equals("Listing Phuong 2")));
    }

    @Test
    @DisplayName("Lọc tiện ích theo quy tắc AND: phải khớp đầy đủ tất cả tiện ích yêu cầu")
    void testFilterAmenitiesWithAndRule() {
        createListing("Has Pool Only", "Address 1", BigDecimal.valueOf(900_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(tienIchPool), LocalDateTime.now());
        createListing("Has Pool and Wifi", "Address 2", BigDecimal.valueOf(950_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(tienIchPool, tienIchWifi), LocalDateTime.now());
        createListing("Has None", "Address 3", BigDecimal.valueOf(800_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());

        ListingFilter filter = new ListingFilter(null, null, null, null, null, List.of(tienIchPool.getMaTienIch(), tienIchWifi.getMaTienIch()), null, null, null, null);
        Specification<CoSoLuuTru> spec = ListingSpecs.forGuests(filter, null, false);
        List<CoSoLuuTru> result = coSoRepo.findAll(spec);

        assertTrue(result.stream().anyMatch(c -> c.getTenCSLT().equals("Has Pool and Wifi")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Has Pool Only")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Has None")));
    }

    @Test
    @DisplayName("Chỉ lấy cơ sở APPROVED và host ACTIVE: loại trừ PENDING/DRAFT/REMOVED và Host LOCKED")
    void testOnlyApprovedAndHostActive() {
        createListing("Approved Active", "Addr 1", BigDecimal.valueOf(800_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Draft Active", "Addr 2", BigDecimal.valueOf(800_000), ListingStatus.DRAFT, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Pending Active", "Addr 3", BigDecimal.valueOf(800_000), ListingStatus.PENDING, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Approved Locked Host", "Addr 4", BigDecimal.valueOf(800_000), ListingStatus.APPROVED, lockedHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());

        ListingFilter filter = new ListingFilter(null, null, null, null, null, null, null, null, null, null);
        Specification<CoSoLuuTru> spec = ListingSpecs.forGuests(filter, null, false);
        List<CoSoLuuTru> result = coSoRepo.findAll(spec);

        assertTrue(result.stream().anyMatch(c -> c.getTenCSLT().equals("Approved Active")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Draft Active")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Pending Active")));
        assertTrue(result.stream().noneMatch(c -> c.getTenCSLT().equals("Approved Locked Host")));
    }

    @Test
    @DisplayName("Tìm kiếm theo từ khóa keyword (tên CSLT hoặc địa chỉ chi tiết)")
    void testKeywordSearch() {
        createListing("Sunrise Hotel", "12 Pham Van Dong", BigDecimal.valueOf(750_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        createListing("Blue Ocean Resort", "88 Tran Phu", BigDecimal.valueOf(1_500_000), ListingStatus.APPROVED, activeHost, phuong1, loaiVilla, List.of(), LocalDateTime.now());

        ListingFilter filterByName = new ListingFilter("Sunrise", null, null, null, null, null, null, null, null, null);
        List<CoSoLuuTru> byName = coSoRepo.findAll(ListingSpecs.forGuests(filterByName, null, false));
        assertTrue(byName.stream().anyMatch(c -> c.getTenCSLT().equals("Sunrise Hotel")));
        assertTrue(byName.stream().noneMatch(c -> c.getTenCSLT().equals("Blue Ocean Resort")));

        ListingFilter filterByAddress = new ListingFilter("Tran Phu", null, null, null, null, null, null, null, null, null);
        List<CoSoLuuTru> byAddress = coSoRepo.findAll(ListingSpecs.forGuests(filterByAddress, null, false));
        assertTrue(byAddress.stream().anyMatch(c -> c.getTenCSLT().equals("Blue Ocean Resort")));
        assertTrue(byAddress.stream().noneMatch(c -> c.getTenCSLT().equals("Sunrise Hotel")));
    }

    @Test
    @DisplayName("Phân trang và sắp xếp: GIA_ASC, GIA_DESC, NGAY_CAP_NHAT_DESC")
    void testPaginationAndSorting() {
        LocalDateTime baseTime = LocalDateTime.now();
        createListing("Sort P1", "Addr", BigDecimal.valueOf(100_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), baseTime.minusDays(3));
        createListing("Sort P2", "Addr", BigDecimal.valueOf(500_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), baseTime.minusDays(1));
        createListing("Sort P3", "Addr", BigDecimal.valueOf(900_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), baseTime.minusDays(2));

        ListingFilter filter = new ListingFilter("Sort P", null, null, null, null, null, null, null, null, null);
        Specification<CoSoLuuTru> spec = ListingSpecs.forGuests(filter, null, false);

        // GIA_ASC
        Sort sortAsc = Sort.by(Sort.Direction.ASC, "gia").and(Sort.by("maCSLT"));
        Page<CoSoLuuTru> pageGiaAsc = coSoRepo.findAll(spec, PageRequest.of(0, 10, sortAsc));
        assertEquals(3, pageGiaAsc.getContent().size());
        assertEquals("Sort P1", pageGiaAsc.getContent().get(0).getTenCSLT());
        assertEquals("Sort P2", pageGiaAsc.getContent().get(1).getTenCSLT());
        assertEquals("Sort P3", pageGiaAsc.getContent().get(2).getTenCSLT());

        // GIA_DESC
        Sort sortDesc = Sort.by(Sort.Direction.DESC, "gia").and(Sort.by("maCSLT"));
        Page<CoSoLuuTru> pageGiaDesc = coSoRepo.findAll(spec, PageRequest.of(0, 10, sortDesc));
        assertEquals("Sort P3", pageGiaDesc.getContent().get(0).getTenCSLT());
        assertEquals("Sort P2", pageGiaDesc.getContent().get(1).getTenCSLT());
        assertEquals("Sort P1", pageGiaDesc.getContent().get(2).getTenCSLT());

        // NGAY_CAP_NHAT_DESC
        Sort sortDate = Sort.by(Sort.Direction.DESC, "ngayCapNhat").and(Sort.by("maCSLT"));
        Page<CoSoLuuTru> pageDate = coSoRepo.findAll(spec, PageRequest.of(0, 10, sortDate));
        assertEquals("Sort P2", pageDate.getContent().get(0).getTenCSLT());
        assertEquals("Sort P3", pageDate.getContent().get(1).getTenCSLT());
        assertEquals("Sort P1", pageDate.getContent().get(2).getTenCSLT());
    }

    @Test
    @DisplayName("findVisibleById: trả về cơ sở khi APPROVED và host ACTIVE; trả về empty nếu chưa duyệt hoặc host khóa")
    void testFindVisibleById() {
        CoSoLuuTru approved = createListing("Visible Listing", "Addr", BigDecimal.valueOf(600_000), ListingStatus.APPROVED, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        CoSoLuuTru draft = createListing("Draft Hidden", "Addr", BigDecimal.valueOf(600_000), ListingStatus.DRAFT, activeHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());
        CoSoLuuTru locked = createListing("Locked Host Hidden", "Addr", BigDecimal.valueOf(600_000), ListingStatus.APPROVED, lockedHost, phuong1, loaiHotel, List.of(), LocalDateTime.now());

        Optional<CoSoLuuTru> visible = coSoRepo.findVisibleById(approved.getMaCSLT());
        assertTrue(visible.isPresent());
        assertEquals(approved.getMaCSLT(), visible.get().getMaCSLT());

        assertTrue(coSoRepo.findVisibleById(draft.getMaCSLT()).isEmpty(), "Cơ sở DRAFT không được hiển thị cho khách");
        assertTrue(coSoRepo.findVisibleById(locked.getMaCSLT()).isEmpty(), "Cơ sở của host bị LOCKED không được hiển thị cho khách");
        assertTrue(coSoRepo.findVisibleById(999999).isEmpty(), "ID không tồn tại phải trả về empty");
    }
}
