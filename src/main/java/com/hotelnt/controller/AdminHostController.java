package com.hotelnt.controller;

import com.hotelnt.common.error.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.AdminIdentityResolver;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/hosts")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "admin-host-controller")
public class AdminHostController {

    private final AdminHostService service;
    private final AdminIdentityResolver identities;

    public AdminHostController(AdminHostService service, AdminIdentityResolver identities) {
        this.service = service;
        this.identities = identities;
    }

    // GET /api/admin/hosts
    @GetMapping
    public HostPageResponse getHosts(
            HttpServletRequest request,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        identities.resolve(request);
        return service.getHosts(trangThai, keyword, page, limit);
    }

    // GET /api/admin/hosts/{hostId}
    @GetMapping("/{hostId}")
    public HostDetail getHostDetail(HttpServletRequest request, @PathVariable int hostId) {
        identities.resolve(request);
        return service.getHostDetail(hostId);
    }

    // PATCH /api/admin/hosts/{hostId}/status
    @PatchMapping("/{hostId}/status")
    public UpdateHostStatusResponse updateHostStatus(
            HttpServletRequest request,
            @PathVariable int hostId,
            @Valid @RequestBody UpdateHostStatusRequest body) {
        identities.resolve(request);
        return service.updateHostStatus(hostId, body);
    }
}
