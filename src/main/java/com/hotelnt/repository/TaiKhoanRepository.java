package com.hotelnt.repository;

import com.hotelnt.domain.TaiKhoan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TaiKhoan> {
    Optional<TaiKhoan> findByEmail(String email);
    Optional<TaiKhoan> findBySoDienThoai(String soDienThoai);
}
