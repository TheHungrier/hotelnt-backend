package com.hotelnt.modules.auth;

import com.hotelnt.domain.TaiKhoan;
import org.springframework.data.repository.Repository;

/** Cac truy van tai khoan can thiet cho buoc dang ky. */
interface SignupAccountRepository extends Repository<TaiKhoan, Integer> {
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    boolean existsByCccd(String cccd);
}
