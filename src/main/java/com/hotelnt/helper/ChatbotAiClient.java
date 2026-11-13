package com.hotelnt.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelnt.dto.ChatbotDtos;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatbotAiClient {

    private static final Logger log = LoggerFactory.getLogger(ChatbotAiClient.class);
    private static final String DEFAULT_MODEL = "google/gemini-2.0-flash-exp:free";
    private static final String DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    private final ObjectMapper objectMapper;

    @Value("${openrouter.api-key:${gemini.api-key:}}")
    private String configuredApiKey;

    @Value("${openrouter.model:${gemini.model:}}")
    private String configuredModel;

    @Value("${openrouter.endpoint:" + DEFAULT_ENDPOINT + "}")
    private String configuredEndpoint;

    public ChatbotAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Goi OpenRouter REST API (chuan OpenAI format) de tao cau tra loi tu nhien cho khach dua tren danh sach CSLT tim thay.
     * Ho tro moi model tren OpenRouter (Gemini, DeepSeek, Claude,...).
     * Tra ve null neu khong the ket noi hoac khong co API key de fallback noi bo an toan.
     */
    public String generateReply(String userMessage, List<ChatbotDtos.SuggestedListing> listings) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Chua cau hinh OPENROUTER_API_KEY (hoac GEMINI_API_KEY), su dung bo tao phan hoi noi bo.");
            return null;
        }

        String model = (configuredModel != null && !configuredModel.isBlank()) ? configuredModel.trim() : DEFAULT_MODEL;
        String endpoint = (configuredEndpoint != null && !configuredEndpoint.isBlank()) ? configuredEndpoint.trim() : DEFAULT_ENDPOINT;

        try {
            String prompt = buildPrompt(userMessage, listings);

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );
            byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("HTTP-Referer", "https://hotelnt.vn");
            conn.setRequestProperty("X-Title", "HotelNT");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                try (InputStream is = conn.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    JsonNode choice = root.path("choices").path(0);
                    JsonNode contentNode = choice.path("message").path("content");
                    if (!contentNode.isMissingNode()) {
                        String text = contentNode.asText().trim();
                        if (!text.isBlank()) {
                            log.info("OpenRouter tra loi thanh cong voi model {}", model);
                            return text;
                        }
                    }
                }
            } else {
                String errorBody = "";
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    try (InputStream is = errorStream) {
                        errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                log.warn("OpenRouter API tra ve ma loi {} voi model {}: {}", status, model, errorBody);
            }
        } catch (Exception e) {
            log.warn("Loi khi goi OpenRouter AI: {}", e.getMessage());
        }

        return null;
    }

    public String buildPrompt(String userMessage, List<ChatbotDtos.SuggestedListing> listings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý AI tư vấn du lịch và lưu trú của nền tảng HotelNT tại thành phố Nha Trang.\n");
        sb.append("QUY TẮC BẮT BUỘC:\n");
        sb.append("1. Chỉ trả lời các nội dung liên quan đến cơ sở lưu trú và du lịch tại Nha Trang.\n");
        sb.append(
                "2. Tuyệt đối không trả lời các câu hỏi ngoài phạm vi dự án (như địa phương khác ngoài Nha Trang, thời tiết ngoài tỉnh, giải toán, lập trình, kiến thức chung không liên quan,...). Khi gặp câu hỏi ngoài phạm vi, hãy từ chối ngắn gọn, lịch sự và hướng dẫn khách quay lại nhu cầu lưu trú tại Nha Trang.\n");
        sb.append(
                "3. Chỉ cung cấp thông tin dựa trên dữ liệu cơ sở lưu trú được hệ thống cung cấp dưới đây; không tự bịa ra cơ sở, giá, tiện nghi hoặc đường dẫn.\n");
        sb.append(
                "4. HotelNT là nền tảng đang được tư vấn. Không quảng bá, đề xuất hoặc so sánh với Agoda, Booking.com, Traveloka hay bất kỳ nền tảng bên ngoài nào. Nếu khách hỏi ứng dụng nào tốt nhất, hãy giới thiệu HotelNT và mời khách nêu tiêu chí tìm phòng.\n");
        sb.append(
                "5. Không tự suy luận rằng cơ sở không có trong dữ liệu là tốt hơn. Chỉ giới thiệu các cơ sở trong danh sách kết quả tra cứu.\n\n");
        sb.append("Câu hỏi của khách: \"").append(userMessage).append("\"\n\n");

        if (listings == null || listings.isEmpty()) {
            sb.append(
                    "Kết quả tra cứu: Hệ thống không tìm thấy cơ sở lưu trú nào phù hợp (hoặc yêu cầu nằm ngoài phạm vi hỗ trợ của HotelNT tại Nha Trang).\n");
            sb.append(
                    "Hãy phản hồi lịch sự, từ chối các nội dung ngoài lề và nhắc nhở khách về phạm vi hỗ trợ lưu trú tại Nha Trang của HotelNT.");
        } else {
            sb.append("Kết quả tra cứu từ hệ thống tìm thấy ").append(listings.size())
                    .append(" cơ sở lưu trú phù hợp:\n");
            for (int i = 0; i < listings.size(); i++) {
                ChatbotDtos.SuggestedListing l = listings.get(i);
                sb.append(i + 1).append(". ").append(l.tenCSLT())
                        .append(" - Loại: ").append(l.tenLoai())
                        .append(" - Giá: ").append(l.gia() != null ? l.gia() + " VNĐ/đêm" : "Liên hệ")
                        .append("\n");
            }
            sb.append(
                    "\nHãy tóm tắt ngắn gọn và giới thiệu những lựa chọn phù hợp nhất này cho khách một cách cuốn hút (khoảng 1-3 câu).");
        }

        return sb.toString();
    }

    private String resolveApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey.trim();
        }
        String env = System.getenv("OPENROUTER_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        try {
            Path envPath = Path.of(".env");
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.startsWith("OPENROUTER_API_KEY=")) {
                        return line.substring("OPENROUTER_API_KEY=".length()).trim();
                    }
                }
                for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.startsWith("GEMINI_API_KEY=")) {
                        return line.substring("GEMINI_API_KEY=".length()).trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
