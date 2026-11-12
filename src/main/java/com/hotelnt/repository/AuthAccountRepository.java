package com.hotelnt.repository;

import com.hotelnt.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuthAccountRepository extends Repository<TaiKhoan, Integer> {
    @Query("select a from TaiKhoan a where a.email = :identity or a.soDienThoai = :identity")
    Optional<TaiKhoan> findByIdentity(@Param("identity") String identity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TaiKhoan a where a.email = :identity or a.soDienThoai = :identity")
    Optional<TaiKhoan> findForUpdate(@Param("identity") String identity);
}
