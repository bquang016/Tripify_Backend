package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {
    private Integer roomId;
    private Integer propertyId;
    private String roomName;
    private RoomCategory roomCategory;
    private BigDecimal area;
    private Integer roomAmount;
    private BigDecimal pricePerNight;
    private BigDecimal weekendPrice;
    private int capacity;
    private String description;
    private RoomStatus roomStatus;
    private boolean active;

    private List<String> images;
    private List<String> amenities;

    public RoomResponseDTO(Room room) {
        this.roomId = room.getRoomId();

        // ✅ Sửa: getProperty()
        if (room.getProperty() != null) {
            this.propertyId = room.getProperty().getPropertyId();
        }

        this.roomName = room.getRoomName();
        this.area = room.getArea();
        this.roomAmount = room.getRoomAmount();
        this.roomCategory = room.getRoomCategory(); // Có thể null
        this.pricePerNight = room.getPricePerNight();
        this.weekendPrice = room.getWeekendPrice();
        this.capacity = room.getCapacity();
        this.description = room.getDescription();
        this.roomStatus = room.getRoomStatus();     // Có thể null
        this.active = room.isActive();

        this.images = new ArrayList<>();
        this.amenities = new ArrayList<>();
    }
}