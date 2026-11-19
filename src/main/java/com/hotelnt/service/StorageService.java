package com.hotelnt.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file, String type);

    void delete(String fileUrl);
}
