package com.hotelnt.repository;

import com.hotelnt.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TaiKhoan> {
    Optional<TaiKhoan> findByEmail(String email);
    Optional<TaiKhoan> findBySoDienThoai(String soDienThoai);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TaiKhoan a where a.id = :id")
    Optional<TaiKhoan> findByIdForUpdate(@Param("id") Integer id);
}
