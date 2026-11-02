package com.hotelnt.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "CoSoLuuTru")
public class CoSoLuuTru {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maCSLT;

    private String tenCSLT;
    private String diaChiChiTiet;
    private String moTa;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal gia;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String lyDoTuChoi;
    private String lyDoViPham;
    private LocalDateTime ngayGuiDuyet;
    private LocalDateTime ngayDuyet;
    private LocalDateTime ngayCapNhatGia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHost")
    private TaiKhoan host;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhuong")
    private Phuong phuong;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maLoai")
    private LoaiCoSo loai;

    @ManyToMany
    @JoinTable(name = "TienIchCSLT",
            joinColumns = @JoinColumn(name = "maCSLT"),
            inverseJoinColumns = @JoinColumn(name = "maTienIch"))
    private Set<TienIch> tienIch = new HashSet<>();

    @OneToMany(mappedBy = "coSo")
    @OrderBy("thuTu ASC")
    private List<Media> media = new ArrayList<>();

    @OneToMany(mappedBy = "coSo")
    private List<LienHe> lienHe = new ArrayList<>();

    public CoSoLuuTru() {}

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
        if (ngayCapNhat == null) ngayCapNhat = now;
    }

    public Integer getMaCSLT() { return maCSLT; }
    public void setMaCSLT(Integer maCSLT) { this.maCSLT = maCSLT; }
    public String getTenCSLT() { return tenCSLT; }
    public void setTenCSLT(String tenCSLT) { this.tenCSLT = tenCSLT; }
    public String getDiaChiChiTiet() { return diaChiChiTiet; }
    public void setDiaChiChiTiet(String diaChiChiTiet) { this.diaChiChiTiet = diaChiChiTiet; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getGia() { return gia; }
    public void setGia(BigDecimal gia) { this.gia = gia; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
    public String getLyDoTuChoi() { return lyDoTuChoi; }
    public void setLyDoTuChoi(String lyDoTuChoi) { this.lyDoTuChoi = lyDoTuChoi; }
    public String getLyDoViPham() { return lyDoViPham; }
    public void setLyDoViPham(String lyDoViPham) { this.lyDoViPham = lyDoViPham; }
    public LocalDateTime getNgayGuiDuyet() { return ngayGuiDuyet; }
    public void setNgayGuiDuyet(LocalDateTime ngayGuiDuyet) { this.ngayGuiDuyet = ngayGuiDuyet; }
    public LocalDateTime getNgayDuyet() { return ngayDuyet; }
    public void setNgayDuyet(LocalDateTime ngayDuyet) { this.ngayDuyet = ngayDuyet; }
    public LocalDateTime getNgayCapNhatGia() { return ngayCapNhatGia; }
    public void setNgayCapNhatGia(LocalDateTime ngayCapNhatGia) { this.ngayCapNhatGia = ngayCapNhatGia; }
    public TaiKhoan getHost() { return host; }
    public void setHost(TaiKhoan host) { this.host = host; }
    public Phuong getPhuong() { return phuong; }
    public void setPhuong(Phuong phuong) { this.phuong = phuong; }
    public LoaiCoSo getLoai() { return loai; }
    public void setLoai(LoaiCoSo loai) { this.loai = loai; }
    public Set<TienIch> getTienIch() { return tienIch; }
    public void setTienIch(Set<TienIch> tienIch) { this.tienIch = tienIch; }
    public List<Media> getMedia() { return media; }
    public void setMedia(List<Media> media) { this.media = media; }
    public List<LienHe> getLienHe() { return lienHe; }
    public void setLienHe(List<LienHe> lienHe) { this.lienHe = lienHe; }
}
