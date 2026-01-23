package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.RoomAmenity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomAmenityResponseDTO {
    private int roomAmenityId;
    private int roomId;
    private int amenityId;
    private String amenityName;
    private boolean active;

    public RoomAmenityResponseDTO(RoomAmenity entity) {
        this.roomAmenityId = entity.getRoomAmenityId();

        if (entity.getRoom() != null) {
            this.roomId = entity.getRoom().getRoomId();
        }

        if (entity.getAmenity() != null) {
            this.amenityId = entity.getAmenity().getAmenityId();
            this.amenityName = entity.getAmenity().getAmenityName();
        }

        this.active = entity.isActive();
    }
}