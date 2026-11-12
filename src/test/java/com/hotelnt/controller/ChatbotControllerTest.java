package com.hotelnt.controller;

import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                "Villa View Bi?n Nha Trang",
                "Villa",
                1500000L,
                Instant.parse("2026-10-10T08:15:00Z"),
                "https://cdn.example.com/listings/1.jpg",
                new ChatContactInfo("0905123456", "https://zalo.me/0905123456", "https://facebook.com/villabienxanh")
        );

        ChatMessageResponse response = new ChatMessageResponse(
                "M�nh t�m du?c 1 co s? ph� h?p v?i y�u c?u c?a b?n:",
                "session-123",
                List.of(listing)
        );

        when(service.handleMessage(any(ChatMessageRequest.class))).thenReturn(response);

        ChatMessageRequest req = new ChatMessageRequest("Cho t�i kh�ch s?n view bi?n gi� du?i 2 tri?u ? Nha Trang", "session-123");

        mvc.perform(post("/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyText").value("M�nh t�m du?c 1 co s? ph� h?p v?i y�u c?u c?a b?n:"))
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.suggestedListings[0].maCSLT").value(1))
                .andExpect(jsonPath("$.suggestedListings[0].tenCSLT").value("Villa View Bi?n Nha Trang"))
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