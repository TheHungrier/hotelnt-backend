package com.hotelnt.repository;

import com.hotelnt.domain.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Cac thao tac thu hoi phien dung chung cho dang xuat va doi mat khau. */
public interface SessionRevocationRepository extends Repository<PhienDangNhap, String> {
    @Modifying(flushAutomatically = true)
    @Query("update PhienDangNhap p set p.daThuHoi = true where p.maPhien = :sessionId and p.daThuHoi = false")
    int revokeSession(@Param("sessionId") String sessionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PhienDangNhap p set p.daThuHoi = true where p.taiKhoan.id = :accountId and p.daThuHoi = false")
    int revokeAccountSessions(@Param("accountId") int accountId);
}
