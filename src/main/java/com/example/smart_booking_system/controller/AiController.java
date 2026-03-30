package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.dto.request.ChatRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> chat(
            @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser // Chỉ cần bắt User xịn
    ) {
        try {
            // 1. CHẶN KHÁCH VÃNG LAI (Bảo mật 2 lớp cùng SecurityConfig)
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Vui lòng đăng nhập để sử dụng tính năng Chatbot AI."));
            }

            // 2. LẤY EMAIL HOẶC USER_ID LÀM SESSION CHO n8n
            // (Tớ dùng getUserId() theo code cũ của bạn, bạn có thể đổi thành getEmail()
            // nếu muốn)
            String sessionId = currentUser.getUsername().toString();

            // 3. GỌI SERVICE XỬ LÝ
            ChatResponseDTO aiResult = aiService.processChat(sessionId, request.getMessage());

            return ResponseEntity.ok(ApiResponse.success("AI trả lời thành công", aiResult));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xử lý AI: " + e.getMessage()));
        }
    }
}