package com.hotelnt.controller;

import com.hotelnt.dto.*;
import com.hotelnt.service.*;
import com.hotelnt.helper.HostIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/host/listings/{maCSLT}/media")
public class MediaController {

    private final MediaService mediaService;
    private final HostIdentityResolver identities;

    public MediaController(MediaService mediaService, HostIdentityResolver identities) {
        this.mediaService = mediaService;
        this.identities = identities;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaDtos.MediaUploadResponse uploadMedia(
            @PathVariable int maCSLT,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "IMAGE") String type,
            HttpServletRequest request) {
        return mediaService.upload(maCSLT, identities.resolve(request).accountId(), file, type);
    }

    @DeleteMapping("/{mediaId}")
    public MediaDtos.MediaDeleteResponse deleteMedia(
            @PathVariable int maCSLT,
            @PathVariable int mediaId,
            HttpServletRequest request) {
        return mediaService.delete(maCSLT, mediaId, identities.resolve(request).accountId());
    }
}
