package com.hotelnt.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "TienIch")
public class TienIch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maTienIch;

    private String tenTienIch;
    private String nhom;
    private String icon;

    public TienIch() {}

    public Integer getMaTienIch() { return maTienIch; }
    public void setMaTienIch(Integer maTienIch) { this.maTienIch = maTienIch; }
    public String getTenTienIch() { return tenTienIch; }
    public void setTenTienIch(String tenTienIch) { this.tenTienIch = tenTienIch; }
    public String getNhom() { return nhom; }
    public void setNhom(String nhom) { this.nhom = nhom; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
