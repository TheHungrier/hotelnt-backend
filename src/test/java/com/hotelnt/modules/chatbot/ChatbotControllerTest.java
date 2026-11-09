package com.hotelnt.modules.chatbot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.modules.chatbot.ChatbotDtos.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatbotController.class)
class ChatbotControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatbotService service;

    @Test
    void sendMessage_successWithSuggestions() throws Exception {
        SuggestedListing listing = new SuggestedListing(
                1,
                "Villa View Biển Nha Trang",
                "Villa",
                1500000L,
                Instant.parse("2026-10-10T08:15:00Z"),
                "https://cdn.example.com/listings/1.jpg",
                new ChatContactInfo("0905123456", "https://zalo.me/0905123456", "https://facebook.com/villabienxanh")
        );

        ChatMessageResponse response = new ChatMessageResponse(
                "Mình tìm được 1 cơ sở phù hợp với yêu cầu của bạn:",
                "session-123",
                List.of(listing)
        );

        when(service.handleMessage(any(ChatMessageRequest.class))).thenReturn(response);

        ChatMessageRequest req = new ChatMessageRequest("Cho tôi khách sạn view biển giá dưới 2 triệu ở Nha Trang", "session-123");

        mvc.perform(post("/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyText").value("Mình tìm được 1 cơ sở phù hợp với yêu cầu của bạn:"))
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.suggestedListings[0].maCSLT").value(1))
                .andExpect(jsonPath("$.suggestedListings[0].tenCSLT").value("Villa View Biển Nha Trang"))
                .andExpect(jsonPath("$.suggestedListings[0].gia").value(1500000))
                .andExpect(jsonPath("$.suggestedListings[0].lienHe.soDienThoai").value("0905123456"));
    }

    @Test
    void sendMessage_emptyMessage_returns400() throws Exception {
        ChatMessageRequest req = new ChatMessageRequest("", "session-123");

        mvc.perform(post("/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void sendMessage_messageTooLong_returns400() throws Exception {
        String longText = "a".repeat(501);
        ChatMessageRequest req = new ChatMessageRequest(longText, "session-123");

        mvc.perform(post("/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
