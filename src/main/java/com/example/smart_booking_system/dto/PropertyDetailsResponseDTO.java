package com.example.smart_booking_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class PropertyDetailsResponseDTO {

    private PropertyResponseDTO property;

    private List<RoomResponseDTO> rooms;

    private List<PropertyAmenityResponseDTO> amenities;

    private List<PropertyImageResponseDTO> images;
}
