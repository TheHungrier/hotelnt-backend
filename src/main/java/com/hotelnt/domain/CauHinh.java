package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CauHinh")
@Getter
@Setter
@NoArgsConstructor
public class CauHinh {

    @Id
    private String khoa;

    private String giaTri;
    private LocalDateTime ngayCapNhat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoan")
    private TaiKhoan taiKhoan;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayCapNhat == null)
            ngayCapNhat = now;
    }
}
