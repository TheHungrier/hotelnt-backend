package com.hotelnt.modules.admin;

import com.hotelnt.modules.admin.AdminDtos.ListingReviewDetail;
import com.hotelnt.modules.admin.AdminDtos.ListingReviewPageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
}
