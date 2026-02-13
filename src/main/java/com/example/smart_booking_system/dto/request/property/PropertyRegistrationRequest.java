package com.example.smart_booking_system.dto.request.property;

import com.example.smart_booking_system.enums.PropertyType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class PropertyRegistrationRequest {

    @NotBlank(message = "Tên chỗ nghỉ không được để trống")
    private String propertyName;

    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;

    @NotNull(message = "Loại hình chỗ nghỉ là bắt buộc")
    private PropertyType propertyType; // HOTEL, RESORT, VILLA, HOMESTAY

    @Min(value = 1, message = "Hạng sao thấp nhất là 1")
    @Max(value = 5, message = "Hạng sao cao nhất là 5")
    private int starRating;

    // --- ĐỊA CHỈ ---
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String address;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String city;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    private String ward;

    // Tọa độ (Có thể null nếu chưa chọn trên map)
    private Double latitude;
    private Double longitude;

    // --- TIỆN ÍCH ---
    // Danh sách ID của các tiện ích đã chọn (VD: [1, 5, 8] tương ứng Wifi, Pool...)
    private List<Integer> amenityIds;
}