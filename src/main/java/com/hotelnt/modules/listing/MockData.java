package com.hotelnt.modules.listing;

import com.hotelnt.modules.filteroption.FilterOptionController.Option;
import com.hotelnt.modules.filteroption.FilterOptionController.PropertyType;
import com.hotelnt.modules.filteroption.FilterOptionController.Ward;
import com.hotelnt.modules.listing.ListingDtos.*;
import java.time.Instant;
import java.util.List;

/** DU LIEU MAU de front end goi duoc ngay. Khi co DB thi xoa file nay va doc tu Repository. */
public final class MockData {
    private MockData() {}

    public static final List<Ward> WARDS = List.of(
            new Ward(1, "Phước Hải"), new Ward(2, "Lộc Thọ"), new Ward(3, "Vĩnh Phước"));
    public static final List<PropertyType> TYPES = List.of(
            new PropertyType(1, "Khách sạn"), new PropertyType(2, "Villa"), new PropertyType(3, "Homestay"));
    public static final List<Option> AMENITIES = List.of(
            new Option(1, "Hồ bơi", "pool"), new Option(2, "Wifi", "wifi"));
    public static final List<Option> NEARBY = List.of(new Option(3, "Gần biển", "beach"));

    private static final Instant T = Instant.parse("2026-10-01T08:00:00Z");
    private static final String IMG = "https://example.com/img/15-1.jpg";
    private static final List<Amenity> HIGHLIGHT = List.of(
            new Amenity(1, "Hồ bơi", "pool"), new Amenity(2, "Wifi", "wifi"));

    public static final List<ListingCard> CARDS = List.of(
            new ListingCard(15, "Villa View Biển Nha Trang", "Villa", "Phước Hải", "12 Trần Phú, Nha Trang",
                    1_500_000, T, 12.2451, 109.1943, IMG, HIGHLIGHT, null));

    public static final List<Marker> MARKERS = List.of(
            new Marker(15, "Villa View Biển Nha Trang", "Villa", 1_500_000, 12.2451, 109.1943, IMG));

    public static final List<ListingDetail> DETAILS = List.of(
            new ListingDetail(15, "Villa View Biển Nha Trang", "Villa", 1, "Phước Hải", "12 Trần Phú, Nha Trang",
                    "Villa sát biển, có hồ bơi riêng.", 12.2451, 109.1943, 1_500_000, T, T, null,
                    new Owner("Nguyễn Văn A"),
                    new Contact("0901234567", null, null),   // truong Host khong nhap => null
                    List.of(new Media(101, IMG, 1)), List.of(),
                    List.of(new AmenityWithGroup(1, "Hồ bơi", "pool", "TIEN_NGHI"),
                            new AmenityWithGroup(3, "Gần biển", "beach", "LAN_CAN"))));
}
