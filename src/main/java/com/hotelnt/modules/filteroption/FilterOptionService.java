package com.hotelnt.modules.filteroption;

import com.hotelnt.modules.filteroption.FilterOptionController.FilterOptionsResponse;
import com.hotelnt.modules.filteroption.FilterOptionController.Option;
import com.hotelnt.modules.filteroption.FilterOptionController.PropertyType;
import com.hotelnt.modules.filteroption.FilterOptionController.Ward;
import com.hotelnt.domain.TienIch;
import com.hotelnt.repository.LoaiCoSoRepository;
import com.hotelnt.repository.PhuongRepository;
import com.hotelnt.repository.TienIchRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FilterOptionService {
    private final PhuongRepository phuongRepo;
    private final LoaiCoSoRepository loaiRepo;
    private final TienIchRepository tienIchRepo;

    public FilterOptionService(PhuongRepository phuongRepo, LoaiCoSoRepository loaiRepo, TienIchRepository tienIchRepo) {
        this.phuongRepo = phuongRepo;
        this.loaiRepo = loaiRepo;
        this.tienIchRepo = tienIchRepo;
    }

    public FilterOptionsResponse get() {
        List<Ward> wards = phuongRepo.findAllByOrderByMaPhuongAsc().stream()
                .map(p -> new Ward(p.getMaPhuong(), p.getTenPhuong())).toList();
        List<PropertyType> types = loaiRepo.findAllByOrderByMaLoaiAsc().stream()
                .map(l -> new PropertyType(l.getMaLoai(), l.getTenLoai())).toList();
        return new FilterOptionsResponse(wards, types, options("TIEN_NGHI"), options("LAN_CAN"));
    }

    private List<Option> options(String nhom) {
        return tienIchRepo.findByNhomOrderByMaTienIchAsc(nhom).stream()
                .map((TienIch t) -> new Option(t.getMaTienIch(), t.getTenTienIch(), t.getIcon())).toList();
    }
}
