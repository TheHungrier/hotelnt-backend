package com.hotelnt.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.dto.ChatbotDtos;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatbotCloudflareClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private ChatbotCloudflareClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
        client = new ChatbotCloudflareClient(restTemplate, objectMapper, "test-account-id", "test-api-token", "@cf/meta/llama-3.1-8b-instruct");
    }

    @Test
    @DisplayName("generateReply thành công: gọi đúng Cloudflare AI URL và parse result.response")
    void generateReply_success_returnsTextFromResponse() {
        String expectedUrl = "https://api.cloudflare.com/client/v4/accounts/test-account-id/ai/run/@cf/meta/llama-3.1-8b-instruct";
        String responseJson = """
                {
                  "result": {
                    "response": "HotelNT xin gợi ý khách sạn Biển Xanh rất thích hợp cho kỳ nghỉ của bạn!"
                  },
                  "success": true,
                  "errors": [],
                  "messages": []
                }
                """;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        ChatbotDtos.SuggestedListing listing = new ChatbotDtos.SuggestedListing(
                1, "Khách sạn Biển Xanh", "Khách sạn", 500000L, null, "https://cdn.example.com/1.jpg", null
        );

        Optional<String> result = client.generateReply("Tìm khách sạn giá 500k", List.of(listing));

        assertThat(result).isPresent();
        assertThat(result.get()).contains("Biển Xanh");
        mockServer.verify();
    }

    @Test
    @DisplayName("generateReply lỗi từ Cloudflare API: trả về Optional.empty() an toàn")
    void generateReply_apiError_returnsEmptySafely() {
        String expectedUrl = "https://api.cloudflare.com/client/v4/accounts/test-account-id/ai/run/@cf/meta/llama-3.1-8b-instruct";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        Optional<String> result = client.generateReply("Tìm phòng", List.of());

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("generateReply khi chưa cấu hình Account ID hoặc Token: trả về Optional.empty() không gọi API")
    void generateReply_missingCredentials_returnsEmptyWithoutCallingApi() {
        ChatbotCloudflareClient unconfiguredClient = new ChatbotCloudflareClient(restTemplate, objectMapper, "", "", "@cf/meta/llama-3.1-8b-instruct");

        Optional<String> result = unconfiguredClient.generateReply("Tìm phòng", List.of());

        assertThat(result).isEmpty();
    }
}
