package com.hotelnt.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PhienDangNhap")
public class PhienDangNhap {

    @Id
    private String maPhien;

    private String refreshTokenHash;
    private LocalDateTime hetHanLuc;
    private Boolean daThuHoi = false;
    private LocalDateTime ngayTao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maTaiKhoan")
    private TaiKhoan taiKhoan;

    public PhienDangNhap() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
    }

    public String getMaPhien() { return maPhien; }
    public void setMaPhien(String maPhien) { this.maPhien = maPhien; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
    public LocalDateTime getHetHanLuc() { return hetHanLuc; }
    public void setHetHanLuc(LocalDateTime hetHanLuc) { this.hetHanLuc = hetHanLuc; }
    public Boolean getDaThuHoi() { return daThuHoi; }
    public void setDaThuHoi(Boolean daThuHoi) { this.daThuHoi = daThuHoi; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }
}
