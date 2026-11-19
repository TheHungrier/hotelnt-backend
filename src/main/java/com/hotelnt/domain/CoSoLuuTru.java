package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "CoSoLuuTru")
@Getter
@Setter
@NoArgsConstructor
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
    @JoinTable(name = "TienIchCSLT", joinColumns = @JoinColumn(name = "maCSLT"), inverseJoinColumns = @JoinColumn(name = "maTienIch"))
    private Set<TienIch> tienIch = new HashSet<>();

    @OneToMany(mappedBy = "coSo")
    @OrderBy("thuTu ASC")
    private List<Media> media = new ArrayList<>();

    @OneToMany(mappedBy = "coSo")
    private List<LienHe> lienHe = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null)
            ngayTao = now;
        if (ngayCapNhat == null)
            ngayCapNhat = now;
    }
}
