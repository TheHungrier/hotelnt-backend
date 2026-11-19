package com.hotelnt.service;

import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.domain.*;
import com.hotelnt.repository.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminListingServiceTest {

    @Mock
    private CoSoLuuTruRepository repo;

    private AdminListingService service;

    @BeforeEach
    void setUp() {
        service = new AdminListingService(repo);
    }

    @Test
    void getListings_draftStatus_throwsValidationError() {
        assertThatThrownBy(() -> service.getListings("DRAFT", 1, 10))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    @SuppressWarnings("unchecked")
                    List<FieldError> details = (List<FieldError>) appEx.getDetails();
                    assertThat(details).hasSize(1);
                    assertThat(details.get(0).field()).isEqualTo("trangThai");
                    assertThat(details.get(0).message()).isEqualTo("Không hỗ trợ DRAFT");
                });

        verifyNoInteractions(repo);
    }

    @Test
    void getListings_invalidStatus_throwsValidationError() {
        assertThatThrownBy(() -> service.getListings("UNKNOWN_STATUS", 1, 10))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    @SuppressWarnings("unchecked")
                    List<FieldError> details = (List<FieldError>) appEx.getDetails();
                    assertThat(details.get(0).field()).isEqualTo("trangThai");
                });

        verifyNoInteractions(repo);
    }

    @Test
    void getListings_invalidPageOrLimit_throwsValidationError() {
        assertThatThrownBy(() -> service.getListings("PENDING", 0, 10))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        assertThatThrownBy(() -> service.getListings("PENDING", 1, 0))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        assertThatThrownBy(() -> service.getListings("PENDING", 1, 101))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(repo);
    }

    @Test
    void getListings_pendingStatus_usesFifoSortAndReturnsPage() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(c), pageRequest, 1));

        ListingReviewPageResponse res = service.getListings("PENDING", 1, 10);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).maCSLT()).isEqualTo(15);
        assertThat(res.items().get(0).tenCSLT()).isEqualTo("Villa Biển Xanh");
        assertThat(res.items().get(0).tenLoai()).isEqualTo("Villa");
        assertThat(res.items().get(0).hoTen()).isEqualTo("Nguyễn Văn A");
        assertThat(res.items().get(0).soDienThoaiHost()).isEqualTo("0905123456");
        assertThat(res.items().get(0).duongDanAnh()).isEqualTo("https://cdn.example.com/img1.jpg");
        assertThat(res.pagination().totalItems()).isEqualTo(1);
        assertThat(res.pagination().currentPage()).isEqualTo(1);

        // Capture pageable to verify FIFO sorting (ngayGuiDuyet ASC)
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repo).findAll(any(Specification.class), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("ngayGuiDuyet")).isNotNull();
        assertThat(sort.getOrderFor("ngayGuiDuyet").isAscending()).isTrue();
    }

    @Test
    void getListingDetail_notFound_throws404() {
        when(repo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getListingDetail(999))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorCode.LISTING_NOT_FOUND));
    }

    @Test
    void getListingDetail_success_computesChecklistAndAllowedActions() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        when(repo.findById(15)).thenReturn(Optional.of(c));

        ListingReviewDetail detail = service.getListingDetail(15);

        assertThat(detail.maCSLT()).isEqualTo(15);
        assertThat(detail.tenCSLT()).isEqualTo("Villa Biển Xanh");
        assertThat(detail.gia()).isEqualTo(1500000L);
        assertThat(detail.checklistDuyet().thongTinBatBuoc()).isTrue();
        assertThat(detail.checklistDuyet().anhToiThieu3()).isTrue();
        assertThat(detail.checklistDuyet().toaDoTrongNhaTrang()).isTrue();
        assertThat(detail.allowedActions()).containsExactly("APPROVE", "REJECT");
        assertThat(detail.chuCoSo().hoTen()).isEqualTo("Nguyễn Văn A");
        assertThat(detail.lienHe().soDienThoai()).isEqualTo("0905123456");
        assertThat(detail.tienIch()).hasSize(1);
        assertThat(detail.media()).hasSize(3);
    }

    private CoSoLuuTru createSampleListing(int id, String status) {
        TaiKhoan host = new TaiKhoan();
        host.setId(3);
        host.setHoTen("Nguyễn Văn A");
        host.setEmail("host01@example.com");
        host.setSoDienThoai("0905123456");
        host.setTrangThai("ACTIVE");

        LoaiCoSo loai = new LoaiCoSo();
        loai.setMaLoai(2);
        loai.setTenLoai("Villa");

        Phuong phuong = new Phuong();
        phuong.setMaPhuong(1);
        phuong.setTenPhuong("Vĩnh Nguyên");

        CoSoLuuTru c = new CoSoLuuTru();
        c.setMaCSLT(id);
        c.setTenCSLT("Villa Biển Xanh");
        c.setDiaChiChiTiet("12 Trần Phú, Nha Trang");
        c.setMoTa("Villa view biển");
        c.setGia(new BigDecimal("1500000"));
        c.setLatitude(new BigDecimal("12.2451"));
        c.setLongitude(new BigDecimal("109.1943"));
        c.setTrangThai(status);
        c.setHost(host);
        c.setLoai(loai);
        c.setPhuong(phuong);
        c.setNgayTao(LocalDateTime.of(2026, 10, 9, 3, 0));
        c.setNgayGuiDuyet(LocalDateTime.of(2026, 10, 11, 7, 45));
        c.setNgayCapNhat(LocalDateTime.of(2026, 10, 11, 7, 45));

        // 3 images
        Media m1 = new Media();
        m1.setMediaId(101);
        m1.setDuongDan("https://cdn.example.com/img1.jpg");
        m1.setLoai("IMAGE");
        m1.setThuTu(0);
        m1.setCoSo(c);

        Media m2 = new Media();
        m2.setMediaId(102);
        m2.setDuongDan("https://cdn.example.com/img2.jpg");
        m2.setLoai("IMAGE");
        m2.setThuTu(1);
        m2.setCoSo(c);

        Media m3 = new Media();
        m3.setMediaId(103);
        m3.setDuongDan("https://cdn.example.com/img3.jpg");
        m3.setLoai("IMAGE");
        m3.setThuTu(2);
        m3.setCoSo(c);

        c.setMedia(List.of(m1, m2, m3));

        // 1 contact
        LienHe lh = new LienHe();
        lh.setMaLienHe(1);
        lh.setLoaiLienHe("SO_DIEN_THOAI");
        lh.setGiaTri("0905123456");
        lh.setCoSo(c);
        c.setLienHe(List.of(lh));

        // 1 amenity
        TienIch t = new TienIch();
        t.setMaTienIch(1);
        t.setTenTienIch("Hồ bơi");
        t.setNhom("TIEN_NGHI");
        c.setTienIch(Set.of(t));

        return c;
    }

    // ------------------------------------------------------------- FR-22 & FR-23
    // Moderate Tests
    @Test
    void moderateListing_notFound_throwsListingNotFound() {
        when(repo.findByIdForUpdate(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moderateListing(999, new ModerateListingRequest("APPROVE", null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorCode.LISTING_NOT_FOUND));
        verify(repo).findByIdForUpdate(999);
    }

    @Test
    void moderateListing_notPending_throwsInvalidStateTransition() {
        CoSoLuuTru c = createSampleListing(15, "APPROVED");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.moderateListing(15, new ModerateListingRequest("APPROVE", null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                });
        verify(repo).findByIdForUpdate(15);
    }

    @Test
    void moderateListing_approveChecklistIncomplete_throwsChecklistIncomplete() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        c.setMedia(List.of()); // 0 ảnh < 3
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.moderateListing(15, new ModerateListingRequest("APPROVE", null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.CHECKLIST_INCOMPLETE);
                    @SuppressWarnings("unchecked")
                    List<FieldError> details = (List<FieldError>) appEx.getDetails();
                    assertThat(details).anyMatch(d -> "hinhAnh".equals(d.field()));
                });
        verify(repo).findByIdForUpdate(15);
    }

    @Test
    void moderateListing_approveSuccess() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        c.setLyDoTuChoi("Lý do cu");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        ModerateListingResponse res = service.moderateListing(15, new ModerateListingRequest("APPROVE", null));

        verify(repo).findByIdForUpdate(15);
        assertThat(res.maCSLT()).isEqualTo(15);
        assertThat(res.trangThai()).isEqualTo("APPROVED");
        assertThat(res.lyDoTuChoi()).isNull();
        assertThat(res.ngayDuyet()).isNotNull();
        assertThat(res.ngayCapNhat()).isNotNull();
        verify(repo).save(c);
        assertThat(c.getTrangThai()).isEqualTo("APPROVED");
        assertThat(c.getLyDoTuChoi()).isNull();
    }

    @Test
    void moderateListing_rejectSuccess() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        ModerateListingResponse res = service.moderateListing(15, new ModerateListingRequest("REJECT", "Ảnh mờ"));

        verify(repo).findByIdForUpdate(15);
        assertThat(res.maCSLT()).isEqualTo(15);
        assertThat(res.trangThai()).isEqualTo("REJECTED");
        assertThat(res.lyDoTuChoi()).isEqualTo("Ảnh mờ");
        assertThat(res.ngayCapNhat()).isNotNull();
        verify(repo).save(c);
        assertThat(c.getTrangThai()).isEqualTo("REJECTED");
        assertThat(c.getLyDoTuChoi()).isEqualTo("Ảnh mờ");
    }

    // ------------------------------------------------------------- FR-24 Unpublish
    // Tests
    @Test
    void unpublishListing_notFound_throwsListingNotFound() {
        when(repo.findByIdForUpdate(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unpublishListing(999, new UnpublishListingRequest("HIDE", "Vi phạm")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorCode.LISTING_NOT_FOUND));
        verify(repo).findByIdForUpdate(999);
    }

    @Test
    void unpublishListing_hideWhenNotApproved_throwsInvalidStateTransition() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.unpublishListing(15, new UnpublishListingRequest("HIDE", "Vi phạm")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                });
        verify(repo).findByIdForUpdate(15);
    }

    @Test
    void unpublishListing_hideSuccess() {
        CoSoLuuTru c = createSampleListing(15, "APPROVED");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        UnpublishListingResponse res = service.unpublishListing(15,
                new UnpublishListingRequest("HIDE", "Thông tin sai"));

        verify(repo).findByIdForUpdate(15);
        assertThat(res.maCSLT()).isEqualTo(15);
        assertThat(res.trangThai()).isEqualTo("HIDDEN");
        assertThat(res.lyDoViPham()).isEqualTo("Thông tin sai");
        assertThat(res.ngayCapNhat()).isNotNull();
        verify(repo).save(c);
        assertThat(c.getTrangThai()).isEqualTo("HIDDEN");
        assertThat(c.getLyDoViPham()).isEqualTo("Thông tin sai");
    }

    @Test
    void unpublishListing_removeSuccess_fromApproved() {
        CoSoLuuTru c = createSampleListing(15, "APPROVED");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        UnpublishListingResponse res = service.unpublishListing(15,
                new UnpublishListingRequest("REMOVE", "Nội dung phần cấm"));

        verify(repo).findByIdForUpdate(15);
        assertThat(res.maCSLT()).isEqualTo(15);
        assertThat(res.trangThai()).isEqualTo("REMOVED");
        assertThat(res.lyDoViPham()).isEqualTo("Nội dung phần cấm");
        verify(repo).save(c);
        assertThat(c.getTrangThai()).isEqualTo("REMOVED");
    }

    @Test
    void unpublishListing_removeSuccess_fromHidden() {
        CoSoLuuTru c = createSampleListing(15, "HIDDEN");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        UnpublishListingResponse res = service.unpublishListing(15,
                new UnpublishListingRequest("REMOVE", "Gỡ vĩnh viễn"));

        verify(repo).findByIdForUpdate(15);
        assertThat(res.maCSLT()).isEqualTo(15);
        assertThat(res.trangThai()).isEqualTo("REMOVED");
        verify(repo).save(c);
    }

    @Test
    void unpublishListing_removeWhenDraftOrPending_throwsInvalidStateTransition() {
        CoSoLuuTru c = createSampleListing(15, "PENDING");
        when(repo.findByIdForUpdate(15)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.unpublishListing(15, new UnpublishListingRequest("REMOVE", "Lý do")))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                });
        verify(repo).findByIdForUpdate(15);
    }
}