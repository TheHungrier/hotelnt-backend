package com.hotelnt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "OtpChallenge")
@Getter
@Setter
@NoArgsConstructor
public class OtpChallenge {

    @Id
    private String challengeId;

    private String loai;
    private String email;
    private String duLieuDangKy;
    private String otpHash;
    private Integer soLanSai = 0;
    private LocalDateTime hetHanLuc;
    private LocalDateTime guiLaiSauLuc;
    private Boolean daDung = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoan")
    private TaiKhoan taiKhoan;
}
