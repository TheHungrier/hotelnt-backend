package com.hotelnt.domain;

/** Gia tri cot TaiKhoan.trangThai va TaiKhoan.vaiTro. */
public final class AccountStatus {
    private AccountStatus() {
    }

    public static final String ACTIVE = "ACTIVE";
    public static final String LOCKED = "LOCKED";
    public static final String ROLE_HOST = "HOST";
    public static final String ROLE_ADMIN = "ADMIN";
}
