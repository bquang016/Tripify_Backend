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

    // Sửa đổi phương thức chat trong AiController.java
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> chat(
            @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId // Frontend gửi lên một ID cố định lưu
                                                                                  // ở localStorage
    ) {
        try {
            String sessionId = (currentUser != null) ? currentUser.getUserId()
                    : (guestId != null ? guestId : "default-guest-session");

            ChatResponseDTO aiResult = aiService.processChat(sessionId, request.getMessage());

            return ResponseEntity.ok(ApiResponse.success("AI trả lời thành công", aiResult));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xử lý AI: " + e.getMessage()));
        }
    }
}
