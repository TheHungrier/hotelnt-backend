package com.hotelnt.repository;

import com.hotelnt.domain.CoSoLuuTru;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoSoLuuTruRepository extends JpaRepository<CoSoLuuTru, Integer>, JpaSpecificationExecutor<CoSoLuuTru> {

    /** Co so khach duoc phep xem: da duyet va chu co so dang hoat dong. */
    @Query("select c from CoSoLuuTru c join fetch c.host h join fetch c.phuong join fetch c.loai "
            + "where c.maCSLT = :id and c.trangThai = 'APPROVED' and h.trangThai = 'ACTIVE'")
    Optional<CoSoLuuTru> findVisibleById(@Param("id") Integer id);
}
