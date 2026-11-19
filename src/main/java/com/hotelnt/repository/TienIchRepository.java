package com.hotelnt.repository;

import com.hotelnt.domain.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TienIchRepository extends JpaRepository<TienIch, Integer> {
    List<TienIch> findByNhomOrderByMaTienIchAsc(String nhom);
}
