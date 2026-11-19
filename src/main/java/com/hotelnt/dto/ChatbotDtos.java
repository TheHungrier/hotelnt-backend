package com.hotelnt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ChatbotDtos {

    private ChatbotDtos() {}

    public record ChatMessageRequest(
            @NotBlank(message = "Tin nhắn không được để trống")
            @Size(max = 500, message = "Tin nhắn không được vượt quá 500 ký tự")
            String message,

            @Size(max = 100, message = "sessionId tối đa 100 ký tự")
            String sessionId
    ) {}

    public record ChatContactInfo(
            String soDienThoai,
            String zalo,
            String facebook
    ) {}

    public record SuggestedListing(
            Integer maCSLT,
            String tenCSLT,
            String tenLoai,
            Long gia,
            Instant ngayCapNhatGia,
            String duongDanAnh,
            ChatContactInfo lienHe
    ) {}

    public record ChatMessageResponse(
            String replyText,
            String sessionId,
            List<SuggestedListing> suggestedListings
    ) {}
}
