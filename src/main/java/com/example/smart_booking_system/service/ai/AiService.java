package com.example.smart_booking_system.service.ai;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    // Đây là đường dẫn Webhook của n8n
    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    public ChatResponseDTO processChat(String sessionId, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new ChatResponseDTO("Dạ anh/chị gửi nội dung giúp em ạ.", "NORMAL_CHAT", null);
        }

        try {
            // 1. Chuẩn bị payload gửi sang n8n
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("sessionId", sessionId); // Để n8n lưu ngữ cảnh hội thoại
            requestBody.put("message", userMessage);

            // 2. Bắn request sang n8n
            WebClient webClient = webClientBuilder.build();
            String n8nResponseRaw = webClient.post()
                    .uri(n8nWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30)) // QUAN TRỌNG: Đợi n8n tối đa 30s để tránh treo BE
                    .block();

            // 3. Phân tích kết quả từ n8n trả về
            JsonNode root = objectMapper.readTree(n8nResponseRaw);

            // Bóc tách JSON theo chuẩn n8n trả ra
            String replyMessage = root.path("replyMessage").asText("Xin lỗi, em không xử lý được.");
            String actionType = root.path("actionType").asText("NORMAL_CHAT");
            JsonNode payload = root.path("payload");

            return new ChatResponseDTO(replyMessage, actionType, payload.isMissingNode() ? null : payload);

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponseDTO("Xin lỗi anh/chị, hệ thống n8n đang bận hoặc phản hồi quá lâu.", "NORMAL_CHAT",
                    null);
        }
    }
}