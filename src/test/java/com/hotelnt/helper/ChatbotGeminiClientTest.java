package com.hotelnt.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.dto.ChatbotDtos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotGeminiClientTest {

    private ChatbotGeminiClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new ChatbotGeminiClient(objectMapper);
    }

    @Test
    @DisplayName("generateReply trả về null an toàn khi chưa cấu hình GEMINI_API_KEY")
    void generateReply_withoutApiKey_returnsNullSafely() {
        ReflectionTestUtils.setField(client, "configuredApiKey", "");
        String reply = client.generateReply("Tìm khách sạn giá rẻ", List.of());
        assertThat(reply).isNull();
    }

    @Test
    @DisplayName("buildPrompt định dạng đúng danh sách CSLT và quy tắc tư vấn Nha Trang")
    void buildPrompt_withListings_containsRulesAndListingDetails() {
        ChatbotDtos.SuggestedListing listing = new ChatbotDtos.SuggestedListing(
                10,
                "Hotel Biển Xanh",
                "Khách sạn",
                500000L,
                null,
                "https://example.com/img.jpg",
                null
        );

        String prompt = client.buildPrompt("Tôi muốn tìm phòng gần biển", List.of(listing));

        assertThat(prompt).contains("HotelNT");
        assertThat(prompt).contains("Nha Trang");
        assertThat(prompt).contains("Hotel Biển Xanh");
        assertThat(prompt).contains("500000 VNĐ/đêm");
        assertThat(prompt).contains("Tôi muốn tìm phòng gần biển");
    }

    @Test
    @DisplayName("buildPrompt khi danh sách rỗng phản hồi lịch sự nhắc nhở phạm vi Nha Trang")
    void buildPrompt_emptyListings_containsFallbackNotice() {
        String prompt = client.buildPrompt("Thời tiết Hà Nội hôm nay thế nào?", List.of());

        assertThat(prompt).contains("không tìm thấy cơ sở lưu trú nào phù hợp");
        assertThat(prompt).contains("Nha Trang");
    }
}
