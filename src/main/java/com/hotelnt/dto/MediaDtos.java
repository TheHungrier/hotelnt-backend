package com.hotelnt.dto;

import com.hotelnt.domain.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
public final class MediaDtos {
    private MediaDtos() {}

    public record MediaUploadResponse(
            int mediaId, String url, String type, int kichThuoc, int thuTu, int soAnh, int soVideo
    ) {}

    public record MediaDeleteResponse(
            String message, int soAnh, int soVideo, boolean thayDoiTrangThai
    ) {}
}
