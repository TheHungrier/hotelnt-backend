package com.hotelnt.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "LienHe")
public class LienHe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maLienHe;

    private String loaiLienHe;
    private String giaTri;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maCSLT")
    private CoSoLuuTru coSo;

    public LienHe() {}

    public Integer getMaLienHe() { return maLienHe; }
    public void setMaLienHe(Integer maLienHe) { this.maLienHe = maLienHe; }
    public String getLoaiLienHe() { return loaiLienHe; }
    public void setLoaiLienHe(String loaiLienHe) { this.loaiLienHe = loaiLienHe; }
    public String getGiaTri() { return giaTri; }
    public void setGiaTri(String giaTri) { this.giaTri = giaTri; }
    public CoSoLuuTru getCoSo() { return coSo; }
    public void setCoSo(CoSoLuuTru coSo) { this.coSo = coSo; }
}
