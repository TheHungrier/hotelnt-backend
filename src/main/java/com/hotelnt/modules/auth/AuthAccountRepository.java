package com.hotelnt.modules.auth;

import com.hotelnt.domain.TaiKhoan;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface AuthAccountRepository extends Repository<TaiKhoan, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TaiKhoan a where a.email = :identity or a.soDienThoai = :identity")
    Optional<TaiKhoan> findForUpdate(@Param("identity") String identity);
}
