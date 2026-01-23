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
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            String userId = (currentUser != null)
                    ? currentUser.getUserId()
                    : "guest-" + java.util.UUID.randomUUID().toString().substring(0, 8);

            ChatResponseDTO aiResult = aiService.processChat(userId, request.getMessage());

            return ResponseEntity.ok(
                    ApiResponse.success("AI trả lời thành công", aiResult)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xử lý AI: " + e.getMessage()));
        }
    }

}

