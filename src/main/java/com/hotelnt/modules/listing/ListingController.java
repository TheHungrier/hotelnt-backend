package com.hotelnt.modules.listing;

import com.hotelnt.modules.listing.ListingDtos.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final ListingService service;

    public ListingController(ListingService service) {
        this.service = service;
    }

    // GET /api/listings  - Tim kiem, loc, sap xep
    @GetMapping
    public ListingPage search(@Valid ListingSearchRequest query) {
        return service.search(query);
    }

    // GET /api/listings/map-markers
    @GetMapping("/map-markers")
    public MarkerResponse mapMarkers() {
        return service.mapMarkers();
    }

    // GET /api/listings/{maCSLT}/preview
    @GetMapping("/{maCSLT}/preview")
    public ListingCard preview(@PathVariable int maCSLT) {
        return service.preview(maCSLT);
    }

    // GET /api/listings/{maCSLT}
    @GetMapping("/{maCSLT}")
    public ListingDetail detail(@PathVariable int maCSLT) {
        return service.detail(maCSLT);
    }
}
