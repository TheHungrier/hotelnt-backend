package com.hotelnt.modules.auth;

import com.hotelnt.domain.PhienDangNhap;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface AuthSessionRepository extends Repository<PhienDangNhap, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhienDangNhap p where p.maPhien = :id")
    Optional<PhienDangNhap> findForUpdate(@Param("id") String id);
}
