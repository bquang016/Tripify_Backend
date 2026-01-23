package com.example.smart_booking_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoomDetailsResponseDTO {

    private RoomResponseDTO room;

    private List<RoomAmenityResponseDTO> amenities;

    private List<RoomImageResponseDTO> images;
}
