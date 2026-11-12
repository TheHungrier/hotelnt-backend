package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "PhienDangNhap")
@Getter
@Setter
@NoArgsConstructor
public class PhienDangNhap {

    @Id
    private String maPhien;

    private String refreshTokenHash;
    private LocalDateTime hetHanLuc;
    private Boolean daThuHoi = false;
    private LocalDateTime ngayTao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maTaiKhoan")
    private TaiKhoan taiKhoan;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null)
            ngayTao = now;
    }
}
