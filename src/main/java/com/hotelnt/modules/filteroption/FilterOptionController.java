package com.hotelnt.modules.filteroption;

import java.util.List;
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

    private final FilterOptionService service;

    public FilterOptionController(FilterOptionService service) {
        this.service = service;
    }

    // GET /api/filter-options
    @GetMapping
    public FilterOptionsResponse get() {
        return service.get();
    }
}
