package com.hotelnt.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan")
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String vaiTro;
    private String trangThai;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String cccd;
    private String matKhau;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String lyDoKhoa;
    private LocalDateTime ngayKhoa;
    private Integer soLanDangNhapSai = 0;
    private LocalDateTime khoaDen;

    public TaiKhoan() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
        if (ngayCapNhat == null) ngayCapNhat = now;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
    public String getLyDoKhoa() { return lyDoKhoa; }
    public void setLyDoKhoa(String lyDoKhoa) { this.lyDoKhoa = lyDoKhoa; }
    public LocalDateTime getNgayKhoa() { return ngayKhoa; }
    public void setNgayKhoa(LocalDateTime ngayKhoa) { this.ngayKhoa = ngayKhoa; }
    public Integer getSoLanDangNhapSai() { return soLanDangNhapSai; }
    public void setSoLanDangNhapSai(Integer soLanDangNhapSai) { this.soLanDangNhapSai = soLanDangNhapSai; }
    public LocalDateTime getKhoaDen() { return khoaDen; }
    public void setKhoaDen(LocalDateTime khoaDen) { this.khoaDen = khoaDen; }
}
