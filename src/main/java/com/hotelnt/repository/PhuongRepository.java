package com.hotelnt.repository;

import com.hotelnt.domain.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhuongRepository extends JpaRepository<Phuong, Integer> {
    List<Phuong> findAllByOrderByMaPhuongAsc();
}
