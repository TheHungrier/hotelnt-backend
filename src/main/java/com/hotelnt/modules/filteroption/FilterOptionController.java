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
    public record PriceRange(long giaMin, long giaMax) {}
    public record SortOption(String value, String label) {}
    public record MapBoundsRange(double latMin, double latMax, double lngMin, double lngMax) {}
    public record FilterOptionsResponse(List<Ward> wards, List<PropertyType> propertyTypes,
                                        List<Option> amenities, List<Option> nearbyPlaces,
                                        PriceRange priceRange, List<Integer> radiusOptions,
                                        List<SortOption> sortOptions, MapBoundsRange mapBounds) {}

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
