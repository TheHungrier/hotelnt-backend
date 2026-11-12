package com.hotelnt.dto;

import java.util.List;

public final class FilterOptionDtos {
    private FilterOptionDtos() {}

    public record PriceRange(long giaMin, long giaMax) {}

    public record SortOption(String value, String label) {}

    public record MapBoundsRange(double latMin, double latMax, double lngMin, double lngMax) {}

    public record FilterOptionsResponse(
            List<FilterOptionDtos.Ward> wards,
            List<FilterOptionDtos.PropertyType> propertyTypes,
            List<Option> amenities,
            List<Option> nearbyPlaces,
            PriceRange priceRange,
            List<Integer> radiusOptions,
            List<SortOption> sortOptions,
            MapBoundsRange mapBounds) {}

    public record Ward(int maPhuong, String tenPhuong) {}

    public record PropertyType(int maLoai, String tenLoai) {}

    public record Option(int maTienIch, String tenTienIch, String icon) {}
}
