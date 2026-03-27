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

            // Bước 3.1: Bóc đúng cái text từ n8n (Giống hệt cách React đã làm)
            String aiText = "";
            if (root.isArray() && root.size() > 0 && root.hasNonNull(0) && root.get(0).has("output")) {
                aiText = root.get(0).get("output").asText();
            } else if (root.has("output")) {
                aiText = root.get("output").asText();
            } else {
                aiText = "Xin lỗi, tôi không thể xử lý câu trả lời lúc này.";
            }

            // Bước 3.2: Khởi tạo giá trị mặc định
            String actionType = "NORMAL_CHAT";
            String finalResponse = aiText;
            JsonNode jsonPayload = null;

            // Bước 3.3: Tuyệt chiêu "Chặt đôi" để lấy PAYLOAD giống React
            if (aiText.contains("PAYLOAD:")) {
                String[] parts = aiText.split("PAYLOAD:");
                finalResponse = parts[0].trim(); // Phần 1: Câu chào

                try {
                    // Phần 2: Cục JSON (Dọn dẹp mớ markdown ```json nếu AI lỡ sinh ra)
                    String jsonString = parts[1].trim()
                            .replaceAll("(?s)```json", "")
                            .replaceAll("(?s)```", "")
                            .trim();

                    // Parse chuỗi thành JsonNode thực thụ để FE dùng được
                    jsonPayload = objectMapper.readTree(jsonString);

                    // Bật cờ để React render HotelResultCard
                    actionType = "SEARCH_ROOM_RESULT";
                } catch (Exception parseEx) {
                    System.err.println("❌ Lỗi Parse JSON từ AI trên Java: " + parseEx.getMessage());
                    finalResponse = "Tôi đã tìm thấy thông tin nhưng gặp lỗi hiển thị. Bạn thử hỏi lại nhé!";
                    actionType = "NORMAL_CHAT";
                }
            }

            // 4. Đóng gói trả về cho Controller
            return new ChatResponseDTO(finalResponse, actionType, jsonPayload);

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponseDTO("Xin lỗi anh/chị, hệ thống n8n đang bận hoặc phản hồi quá lâu.", "NORMAL_CHAT",
                    null);
        }
    }
}