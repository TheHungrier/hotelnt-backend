package com.hotelnt.domain;

import jakarta.persistence.*;


@Entity
@Table(name = "LoaiCoSo")
public class LoaiCoSo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maLoai;

    private String tenLoai;

    public LoaiCoSo() {}

    public Integer getMaLoai() { return maLoai; }
    public void setMaLoai(Integer maLoai) { this.maLoai = maLoai; }
    public String getTenLoai() { return tenLoai; }
    public void setTenLoai(String tenLoai) { this.tenLoai = tenLoai; }
}
