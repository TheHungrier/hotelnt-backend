package com.hotelnt.service;

import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.domain.*;
import com.hotelnt.repository.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminHostServiceTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepo;

    @Mock
    private CoSoLuuTruRepository coSoRepo;

    @Mock
    private PhienDangNhapRepository phienRepo;

    private AdminHostService service;

    @BeforeEach
    void setUp() {
        service = new AdminHostService(taiKhoanRepo, coSoRepo, phienRepo);
    }

    @Test
    void getHosts_validStatus_returnsPage() {
        TaiKhoan tk = new TaiKhoan();
        tk.setId(3);
        tk.setHoTen("Nguyễn Văn A");
        tk.setEmail("host01@example.com");
        tk.setSoDienThoai("0905123456");
        tk.setTrangThai("ACTIVE");
        tk.setVaiTro("HOST");
        tk.setNgayTao(LocalDateTime.now());

        when(taiKhoanRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tk)));
        when(coSoRepo.countByHostId(3)).thenReturn(2L);

        HostPageResponse res = service.getHosts("ACTIVE", null, 1, 10);

        assertEquals(1, res.items().size());
        assertEquals("Nguyễn Văn A", res.items().get(0).hoTen());
        assertEquals(2L, res.items().get(0).soLuongCSLT());
    }

    @Test
    void getHosts_invalidStatus_throwsValidationException() {
        AppException ex = assertThrows(AppException.class, () -> service.getHosts("UNKNOWN", null, 1, 10));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    @Test
    void getHostDetail_success_masksCccdAndBuildsStats() {
        TaiKhoan tk = new TaiKhoan();
        tk.setId(3);
        tk.setHoTen("Nguyễn Văn A");
        tk.setEmail("host01@example.com");
        tk.setSoDienThoai("0905123456");
        tk.setCccd("079123456234");
        tk.setTrangThai("ACTIVE");
        tk.setVaiTro("HOST");

        when(taiKhoanRepo.findById(3)).thenReturn(Optional.of(tk));
        when(coSoRepo.countByHostId(3)).thenReturn(3L);
        when(coSoRepo.countByHostIdGroupByTrangThai(3)).thenReturn(List.of(
                new Object[] { "APPROVED", 2L },
                new Object[] { "DRAFT", 1L }));

        HostDetail detail = service.getHostDetail(3);

        assertEquals(3, detail.id());
        assertEquals("079******234", detail.cccd());
        assertEquals(3L, detail.soLuongCSLT());
        assertEquals(2L, detail.thongKeCSLT().get("APPROVED"));
        assertEquals(1L, detail.thongKeCSLT().get("DRAFT"));
        assertEquals(0L, detail.thongKeCSLT().get("PENDING"));
    }

    @Test
    void getHostDetail_notHost_throwsNotFound() {
        TaiKhoan admin = new TaiKhoan();
        admin.setId(1);
        admin.setVaiTro("ADMIN");

        when(taiKhoanRepo.findById(1)).thenReturn(Optional.of(admin));

        AppException ex = assertThrows(AppException.class, () -> service.getHostDetail(1));
        assertEquals(ErrorCode.HOST_NOT_FOUND, ex.getCode());
    }

    @Test
    void updateHostStatus_lockHost_revokesSessionsAndUpdates() {
        TaiKhoan host = new TaiKhoan();
        host.setId(3);
        host.setVaiTro("HOST");
        host.setTrangThai("ACTIVE");

        when(taiKhoanRepo.findByIdForUpdate(3)).thenReturn(Optional.of(host));

        UpdateHostStatusRequest req = new UpdateHostStatusRequest("LOCKED", "Vi phạm chính sách");
        UpdateHostStatusResponse res = service.updateHostStatus(3, req);

        verify(taiKhoanRepo).findByIdForUpdate(3);
        assertEquals("LOCKED", res.trangThai());
        assertEquals("Vi phạm chính sách", res.lyDoKhoa());
        assertEquals(AccountStatus.LOCKED, host.getTrangThai());
        assertNotNull(host.getNgayKhoa());
        verify(phienRepo).revokeAllByAccountId(3);
        verify(taiKhoanRepo).save(host);
    }

    @Test
    void updateHostStatus_cannotLockAdmin_throwsForbidden() {
        TaiKhoan admin = new TaiKhoan();
        admin.setId(1);
        admin.setVaiTro("ADMIN");
        admin.setTrangThai("ACTIVE");

        when(taiKhoanRepo.findByIdForUpdate(1)).thenReturn(Optional.of(admin));

        UpdateHostStatusRequest req = new UpdateHostStatusRequest("LOCKED", "Khóa thẻ");
        AppException ex = assertThrows(AppException.class, () -> service.updateHostStatus(1, req));
        verify(taiKhoanRepo).findByIdForUpdate(1);
        assertEquals(ErrorCode.CANNOT_LOCK_ADMIN, ex.getCode());
    }

    @Test
    void updateHostStatus_sameStatus_throwsStateUnchanged() {
        TaiKhoan host = new TaiKhoan();
        host.setId(3);
        host.setVaiTro("HOST");
        host.setTrangThai("LOCKED");

        when(taiKhoanRepo.findByIdForUpdate(3)).thenReturn(Optional.of(host));

        UpdateHostStatusRequest req = new UpdateHostStatusRequest("LOCKED", "Khóa tiếp");
        AppException ex = assertThrows(AppException.class, () -> service.updateHostStatus(3, req));
        verify(taiKhoanRepo).findByIdForUpdate(3);
        assertEquals(ErrorCode.STATE_UNCHANGED, ex.getCode());
    }
}