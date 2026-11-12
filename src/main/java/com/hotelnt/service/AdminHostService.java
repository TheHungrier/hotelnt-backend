package com.hotelnt.service;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminHostService {

    private final TaiKhoanRepository taiKhoanRepo;
    private final CoSoLuuTruRepository coSoRepo;
    private final PhienDangNhapRepository phienRepo;

    public AdminHostService(TaiKhoanRepository taiKhoanRepo,
                            CoSoLuuTruRepository coSoRepo,
                            PhienDangNhapRepository phienRepo) {
        this.taiKhoanRepo = taiKhoanRepo;
        this.coSoRepo = coSoRepo;
        this.phienRepo = phienRepo;
    }

    @Transactional(readOnly = true)
    public HostPageResponse getHosts(String trangThai, String keyword, int page, int limit) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;

        if (trangThai != null && !trangThai.isBlank()) {
            String norm = trangThai.trim().toUpperCase();
            if (!AccountStatus.ACTIVE.equals(norm) && !AccountStatus.LOCKED.equals(norm)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, List.of(
                        Map.of("field", "trangThai", "message", "Chỉ nhận ACTIVE hoặc LOCKED")
                ));
            }
        }

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "ngayTao"));
        Page<TaiKhoan> pageResult = taiKhoanRepo.findAll(AdminSpecs.hostFilter(trangThai, keyword), pageable);

        List<HostListItem> items = pageResult.getContent().stream().map(tk -> {
            long soLuongCSLT = coSoRepo.countByHostId(tk.getId());
            return new HostListItem(
                    tk.getId(),
                    tk.getHoTen(),
                    tk.getEmail(),
                    tk.getSoDienThoai(),
                    tk.getTrangThai(),
                    soLuongCSLT,
                    tk.getLyDoKhoa(),
                    Times.toInstant(tk.getNgayTao())
            );
        }).toList();

        Pagination pagination = new Pagination(
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                page,
                limit
        );

        return new HostPageResponse(items, pagination);
    }

    @Transactional(readOnly = true)
    public HostDetail getHostDetail(int hostId) {
        TaiKhoan tk = taiKhoanRepo.findById(hostId)
                .orElseThrow(() -> new AppException(ErrorCode.HOST_NOT_FOUND));

        if (!AccountStatus.ROLE_HOST.equalsIgnoreCase(tk.getVaiTro())) {
            throw new AppException(ErrorCode.HOST_NOT_FOUND);
        }

        long soLuongCSLT = coSoRepo.countByHostId(hostId);

        Map<String, Long> thongKe = new LinkedHashMap<>();
        thongKe.put("DRAFT", 0L);
        thongKe.put("PENDING", 0L);
        thongKe.put("APPROVED", 0L);
        thongKe.put("REJECTED", 0L);
        thongKe.put("HIDDEN", 0L);

        List<Object[]> rawStats = coSoRepo.countByHostIdGroupByTrangThai(hostId);
        for (Object[] row : rawStats) {
            String st = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            if (st != null) {
                thongKe.put(st.toUpperCase(), count);
            }
        }

        return new HostDetail(
                tk.getId(),
                tk.getHoTen(),
                tk.getEmail(),
                tk.getSoDienThoai(),
                maskCccd(tk.getCccd()),
                tk.getTrangThai(),
                tk.getLyDoKhoa(),
                Times.toInstant(tk.getNgayKhoa()),
                Times.toInstant(tk.getNgayTao()),
                soLuongCSLT,
                thongKe
        );
    }

    @Transactional
    public UpdateHostStatusResponse updateHostStatus(int hostId, UpdateHostStatusRequest request) {
        String newStatus = request.trangThai() != null ? request.trangThai().trim().toUpperCase() : "";
        if (!AccountStatus.ACTIVE.equals(newStatus) && !AccountStatus.LOCKED.equals(newStatus)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, List.of(
                    Map.of("field", "trangThai", "message", "Chỉ nhận ACTIVE hoặc LOCKED")
            ));
        }

        TaiKhoan tk = taiKhoanRepo.findByIdForUpdate(hostId)
                .orElseThrow(() -> new AppException(ErrorCode.HOST_NOT_FOUND));

        if (AccountStatus.ROLE_ADMIN.equalsIgnoreCase(tk.getVaiTro())) {
            throw new AppException(ErrorCode.CANNOT_LOCK_ADMIN);
        }

        if (newStatus.equalsIgnoreCase(tk.getTrangThai())) {
            throw new AppException(ErrorCode.STATE_UNCHANGED);
        }

        LocalDateTime now = LocalDateTime.now();
        tk.setTrangThai(newStatus);
        tk.setNgayCapNhat(now);

        if (AccountStatus.LOCKED.equals(newStatus)) {
            tk.setLyDoKhoa(request.lyDoKhoa());
            tk.setNgayKhoa(now);
            phienRepo.revokeAllByAccountId(hostId);
        } else {
            tk.setLyDoKhoa(null);
            tk.setNgayKhoa(null);
        }

        taiKhoanRepo.save(tk);

        return new UpdateHostStatusResponse(
                tk.getId(),
                tk.getTrangThai(),
                tk.getLyDoKhoa(),
                Times.toInstant(now)
        );
    }

    private String maskCccd(String cccd) {
        if (cccd == null || cccd.isBlank()) return null;
        int len = cccd.length();
        if (len <= 6) return "******";
        return cccd.substring(0, 3) + "*".repeat(len - 6) + cccd.substring(len - 3);
    }
}
