package com.hotelnt.controller;

import com.hotelnt.dto.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final ListingService service;

    public ListingController(ListingService service) {
        this.service = service;
    }

    // GET /api/listings - Tim kiem, loc, sap xep
    @GetMapping
    public ListingPage search(@Valid ListingSearchRequest query) {
        query.validateCrossFields();
        return service.search(query);
    }

    // GET /api/listings/map-markers
    @GetMapping("/map-markers")
    public MarkerResponse mapMarkers(@Valid MapMarkerRequest query) {
        query.validateCrossFields();
        MapBounds bounds = MapBounds.parse(query.boundsNorthEast(), query.boundsSouthWest());
        return service.mapMarkers(query, bounds);
    }

    // GET /api/listings/{maCSLT}/preview
    @GetMapping("/{maCSLT}/preview")
    public ListingCard preview(@PathVariable int maCSLT,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return service.preview(maCSLT, latitude, longitude);
    }

    // GET /api/listings/{maCSLT}
    @GetMapping("/{maCSLT}")
    public ListingDetail detail(@PathVariable int maCSLT,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return service.detail(maCSLT, latitude, longitude);
    }
}
