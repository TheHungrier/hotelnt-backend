package com.hotelnt.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "Phuong")
public class Phuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maPhuong;

    private String tenPhuong;

    public Phuong() {}

    public Integer getMaPhuong() { return maPhuong; }
    public void setMaPhuong(Integer maPhuong) { this.maPhuong = maPhuong; }
    public String getTenPhuong() { return tenPhuong; }
    public void setTenPhuong(String tenPhuong) { this.tenPhuong = tenPhuong; }
}
