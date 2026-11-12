package com.hotelnt.repository;

import com.hotelnt.domain.*;
import org.springframework.data.repository.Repository;

/** Cac truy van tai khoan can thiet cho buoc dang ky. */
public interface SignupAccountRepository extends Repository<TaiKhoan, Integer> {
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    boolean existsByCccd(String cccd);
}
