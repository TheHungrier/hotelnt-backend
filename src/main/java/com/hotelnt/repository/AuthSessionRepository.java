package com.hotelnt.repository;

import com.hotelnt.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends Repository<PhienDangNhap, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhienDangNhap p where p.maPhien = :id")
    Optional<PhienDangNhap> findForUpdate(@Param("id") String id);
}
