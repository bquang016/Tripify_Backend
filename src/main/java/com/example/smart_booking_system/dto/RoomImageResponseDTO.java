package com.example.smart_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomImageResponseDTO {

    private int roomImageId;
    private int roomId;
    private String imageUrl;
    private boolean active;
    private boolean isCover;
}
