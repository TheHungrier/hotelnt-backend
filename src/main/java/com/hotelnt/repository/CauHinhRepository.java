package com.hotelnt.repository;

import com.hotelnt.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CauHinhRepository extends JpaRepository<CauHinh, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CauHinh c where c.khoa = :key")
    java.util.Optional<CauHinh> findByIdForUpdate(@Param("key") String key);
}
