package com.hotelnt.modules.filteroption;

import com.hotelnt.modules.listing.MockData;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/filter-options")
public class FilterOptionController {

    public record Ward(int maPhuong, String tenPhuong) {}
    public record PropertyType(int maLoai, String tenLoai) {}
    public record Option(int maTienIch, String tenTienIch, String icon) {}
    public record FilterOptionsResponse(List<Ward> wards, List<PropertyType> propertyTypes,
                                        List<Option> amenities, List<Option> nearbyPlaces) {}

    // GET /api/filter-options
    @GetMapping
    public FilterOptionsResponse get() {
        // TODO: doc tu bang Phuong, LoaiCoSo, TienIch (nhom TIEN_NGHI / LAN_CAN)
        return new FilterOptionsResponse(MockData.WARDS, MockData.TYPES, MockData.AMENITIES, MockData.NEARBY);
    }
}
