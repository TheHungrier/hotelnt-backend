package com.hotelnt.modules.media;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/host/listings/{maCSLT}/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaDtos.MediaUploadResponse uploadMedia(
            @PathVariable int maCSLT,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "IMAGE") String type,
            // TODO: Chờ module Auth của Thiên Phú hoàn tất.
            // Sử dụng header giả X-Mock-Host-Id (mặc định host 2 theo dữ liệu mẫu seed.sql) để test độc lập
            @RequestHeader(value = "X-Mock-Host-Id", defaultValue = "2") Integer hostId) {
        return mediaService.upload(maCSLT, hostId, file, type);
    }

    @DeleteMapping("/{mediaId}")
    public MediaDtos.MediaDeleteResponse deleteMedia(
            @PathVariable int maCSLT,
            @PathVariable int mediaId,
            // TODO: Chờ module Auth của Thiên Phú hoàn tất.
            @RequestHeader(value = "X-Mock-Host-Id", defaultValue = "2") Integer hostId) {
        return mediaService.delete(maCSLT, mediaId, hostId);
    }
}
