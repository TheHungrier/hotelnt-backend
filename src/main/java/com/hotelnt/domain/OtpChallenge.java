package com.hotelnt.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "OtpChallenge")
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

    public OtpChallenge() {}

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDuLieuDangKy() { return duLieuDangKy; }
    public void setDuLieuDangKy(String duLieuDangKy) { this.duLieuDangKy = duLieuDangKy; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Integer getSoLanSai() { return soLanSai; }
    public void setSoLanSai(Integer soLanSai) { this.soLanSai = soLanSai; }
    public LocalDateTime getHetHanLuc() { return hetHanLuc; }
    public void setHetHanLuc(LocalDateTime hetHanLuc) { this.hetHanLuc = hetHanLuc; }
    public LocalDateTime getGuiLaiSauLuc() { return guiLaiSauLuc; }
    public void setGuiLaiSauLuc(LocalDateTime guiLaiSauLuc) { this.guiLaiSauLuc = guiLaiSauLuc; }
    public Boolean getDaDung() { return daDung; }
    public void setDaDung(Boolean daDung) { this.daDung = daDung; }
    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }
}
