package com.hotelnt.repository;

import com.hotelnt.domain.LoaiCoSo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoaiCoSoRepository extends JpaRepository<LoaiCoSo, Integer> {
    List<LoaiCoSo> findAllByOrderByMaLoaiAsc();
}
