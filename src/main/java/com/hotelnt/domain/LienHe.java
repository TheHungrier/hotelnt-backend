package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "LienHe")
@Getter
@Setter
@NoArgsConstructor
public class LienHe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer maLienHe;

    private String loaiLienHe;
    private String giaTri;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maCSLT")
    private CoSoLuuTru coSo;
}
