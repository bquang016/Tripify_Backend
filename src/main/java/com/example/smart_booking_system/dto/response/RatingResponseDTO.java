package com.example.smart_booking_system.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
public class RatingResponseDTO {
    private int ratingId;
    private int stars;        // Map từ field 'rating'
    private String comment;
    private List<String> images; // Chỉ trả về list URL ảnh
    private int bookingId;
    private String propertyName; // Tên khách sạn
    private String roomName;     // Tên phòng
    private String userAvatar;   // Avatar người đánh giá (để hiển thị lên UI)
    private String userName;     // Tên người đánh giá
    private String reply;        // Phản hồi của chủ nhà (nếu có sau này)
    private boolean isHidden;
    private String userId;
    private LocalDateTime createdAt;
    private Boolean isPinned;
}