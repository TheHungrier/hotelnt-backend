package com.hotelnt.repository;

import com.hotelnt.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhienDangNhapRepository extends JpaRepository<PhienDangNhap, String> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PhienDangNhap p set p.daThuHoi = true where p.taiKhoan.id = :accountId and p.daThuHoi = false")
    int revokeAllByAccountId(@org.springframework.data.repository.query.Param("accountId") Integer accountId);
}
