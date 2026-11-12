package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan")
@Getter
@Setter
@NoArgsConstructor
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


    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
        if (ngayCapNhat == null) ngayCapNhat = now;
    }
}
