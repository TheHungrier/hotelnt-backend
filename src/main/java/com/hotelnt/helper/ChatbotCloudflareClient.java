package com.hotelnt.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.dto.ChatbotDtos;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "cloudflare")
public class ChatbotCloudflareClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(ChatbotCloudflareClient.class);
    private static final String DEFAULT_MODEL = "@cf/meta/llama-3.1-8b-instruct";
    private static final String BASE_URL = "https://api.cloudflare.com/client/v4/accounts";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloudflare.ai.account-id:${CF_ACCOUNT_ID:}}")
    private String accountId;

    @Value("${cloudflare.ai.api-token:${CF_API_TOKEN:}}")
    private String apiToken;

    @Value("${cloudflare.ai.model:${CF_AI_MODEL:" + DEFAULT_MODEL + "}}")
    private String model;

    @Autowired
    public ChatbotCloudflareClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(8))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = objectMapper;
    }

    public ChatbotCloudflareClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ChatbotCloudflareClient(RestTemplate restTemplate, ObjectMapper objectMapper,
                                  String accountId, String apiToken, String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.model = model;
    }

    @Override
    public Optional<String> generateReply(String userMessage, List<ChatbotDtos.SuggestedListing> suggested) {
        String accId = (accountId != null) ? accountId.trim() : "";
        String token = (apiToken != null) ? apiToken.trim() : "";
        String aiModel = (model != null && !model.isBlank()) ? model.trim() : DEFAULT_MODEL;

        if (accId.isBlank() || token.isBlank()) {
            log.warn("Chua cau hinh CF_ACCOUNT_ID hoac CF_API_TOKEN cho Cloudflare AI");
            return Optional.empty();
        }

        String url = String.format("%s/%s/ai/run/%s", BASE_URL, accId, aiModel);

        try {
            String systemPrompt = buildSystemPrompt();
            String userContent = buildUserContent(userMessage, suggested);

            Map<String, Object> body = Map.of(
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(URI.create(url), requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode respNode = root.path("result").path("response");
                if (!respNode.isMissingNode()) {
                    String text = respNode.asText().trim();
                    if (!text.isBlank()) {
                        return Optional.of(text);
                    }
                }
            } else {
                log.warn("Cloudflare AI tra ve ma loi: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            // Bao mat: TUYET DOI KHONG log token!
            log.warn("Loi khi goi Cloudflare AI: {}", e.getMessage());
        }

        return Optional.empty();
    }

    public String buildSystemPrompt() {
        return "Bạn là trợ lý tư vấn du lịch và lưu trú của nền tảng HotelNT tại Nha Trang.\n"
                + "QUY TẮC BẮT BUỘC:\n"
                + "1. Chỉ trả lời bằng tiếng Việt, ngắn gọn, thân thiện (khoảng 1-3 câu).\n"
                + "2. Chỉ tư vấn về cơ sở lưu trú và du lịch tại thành phố Nha Trang.\n"
                + "3. Chỉ giới thiệu các cơ sở lưu trú có trong danh sách được cung cấp. Tuyệt đối không tự bịa ra cơ sở, giá cả hoặc địa chỉ.\n"
                + "4. Không quảng bá hay so sánh với các nền tảng khác.";
    }

    public String buildUserContent(String userMessage, List<ChatbotDtos.SuggestedListing> suggested) {
        StringBuilder sb = new StringBuilder();
        if (suggested == null || suggested.isEmpty()) {
            sb.append("Danh sách cơ sở lưu trú phù hợp: (Không có cơ sở nào phù hợp).\n");
        } else {
            sb.append("Danh sách cơ sở lưu trú phù hợp từ hệ thống:\n");
            for (int i = 0; i < suggested.size(); i++) {
                ChatbotDtos.SuggestedListing l = suggested.get(i);
                sb.append(i + 1).append(". ").append(l.tenCSLT())
                        .append(" - Loại: ").append(l.tenLoai() != null ? l.tenLoai() : "Chưa rõ")
                        .append(" - Giá: ").append(l.gia() != null ? l.gia() + " VNĐ/đêm" : "Liên hệ")
                        .append("\n");
            }
        }
        sb.append("\nCâu hỏi của khách hàng: \"").append(userMessage).append("\"");
        return sb.toString();
    }
}
