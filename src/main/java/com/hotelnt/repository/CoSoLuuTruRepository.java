package com.hotelnt.repository;

import com.hotelnt.domain.CoSoLuuTru;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoSoLuuTruRepository extends JpaRepository<CoSoLuuTru, Integer>, JpaSpecificationExecutor<CoSoLuuTru> {

    /** Co so khach duoc phep xem: da duyet va chu co so dang hoat dong. */
    @Query("select c from CoSoLuuTru c join fetch c.host h join fetch c.phuong join fetch c.loai "
            + "where c.maCSLT = :id and c.trangThai = 'APPROVED' and h.trangThai = 'ACTIVE'")
    Optional<CoSoLuuTru> findVisibleById(@Param("id") Integer id);

    /** Lay CoSoLuuTru kem theo Pessimistic Write Lock (UPDLOCK, ROWLOCK tren SQL Server) de chong race condition. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CoSoLuuTru c where c.maCSLT = :id")
    Optional<CoSoLuuTru> findByIdForUpdate(@Param("id") Integer id);


    long countByHostId(Integer hostId);

    @Query("select c.trangThai, count(c) from CoSoLuuTru c where c.host.id = :hostId group by c.trangThai")
    java.util.List<Object[]> countByHostIdGroupByTrangThai(@Param("hostId") Integer hostId);
}
