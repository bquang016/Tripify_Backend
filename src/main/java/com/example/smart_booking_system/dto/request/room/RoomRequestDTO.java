package com.example.smart_booking_system.dto.request.room;

import com.example.smart_booking_system.enums.RoomCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomRequestDTO {

    @NotNull(message = "Property ID là bắt buộc")
    private Integer propertyId;

    @NotBlank(message = "Tên phòng không được để trống")
    private String roomName;

    @NotNull(message = "Loại phòng là bắt buộc")
    private RoomCategory roomCategory; // SINGLE, DOUBLE, FAMILY, SUITE...

    @NotNull(message = "Giá phòng là bắt buộc")
    @Min(value = 0, message = "Giá phòng phải lớn hơn 0")
    private BigDecimal pricePerNight;

    @Min(value = 0, message = "Giá cuối tuần phải lớn hơn 0")
    private BigDecimal weekendPrice;

    @Min(value = 1, message = "Sức chứa phải ít nhất 1 người")
    private int capacity;

    private String description;

    private BigDecimal area; // Diện tích phòng (nếu có)

    // Danh sách ID của tiện nghi được chọn (vd: ["wifi", "tv", "ac"])
    private List<String> amenities;
}