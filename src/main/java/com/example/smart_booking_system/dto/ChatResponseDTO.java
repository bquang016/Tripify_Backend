package com.example.smart_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponseDTO {

    // Câu trả lời dạng text để hiển thị trong khung chat (nếu có)
    private String response;

    // Loại phản hồi: "NORMAL_CHAT", "SEARCH_ROOM_RESULT", "BOOKING_LINK", "ERROR", ...
    private String action;

    // Dữ liệu thêm cho FE: có thể là JsonNode, Map, DTO tuỳ ý
    private Object payload;
}
