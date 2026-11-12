package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "Media")
@Getter
@Setter
@NoArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mediaId;

    private String duongDan;
    private Integer kichThuoc;
    private String loai;
    private String mimeType;
    private Integer thuTu;
    private LocalDateTime ngayTao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maCSLT")
    private CoSoLuuTru coSo;


    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
    }
}
