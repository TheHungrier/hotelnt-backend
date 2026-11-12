package com.hotelnt.controller;

import com.hotelnt.common.error.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.AdminIdentityResolver;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/listings")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "admin-listing-controller")
public class AdminListingController {

    private final AdminListingService service;
    private final AdminIdentityResolver identities;

    public AdminListingController(AdminListingService service, AdminIdentityResolver identities) {
        this.service = service;
        this.identities = identities;
    }

    // GET /api/admin/listings (FR-21)
    @GetMapping
    public ListingReviewPageResponse getListings(
            HttpServletRequest request,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        identities.resolve(request);
        return service.getListings(trangThai, page, limit);
    }

    // GET /api/admin/listings/{maCSLT} (FR-21 review mode)
    @GetMapping("/{maCSLT}")
    public ListingReviewDetail getListingDetail(
            HttpServletRequest request,
            @PathVariable int maCSLT) {
        identities.resolve(request);
        return service.getListingDetail(maCSLT);
    }

    // PATCH /api/admin/listings/{maCSLT}/moderation (FR-22, FR-23)
    @PatchMapping("/{maCSLT}/moderation")
    public ModerateListingResponse moderateListing(
            HttpServletRequest request,
            @PathVariable int maCSLT,
            @Valid @RequestBody ModerateListingRequest body) {
        identities.resolve(request);
        return service.moderateListing(maCSLT, body);
    }

    // PATCH /api/admin/listings/{maCSLT}/unpublish (FR-24)
    @PatchMapping("/{maCSLT}/unpublish")
    public UnpublishListingResponse unpublishListing(
            HttpServletRequest request,
            @PathVariable int maCSLT,
            @Valid @RequestBody UnpublishListingRequest body) {
        identities.resolve(request);
        return service.unpublishListing(maCSLT, body);
    }
}
