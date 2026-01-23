package com.example.smart_booking_system.dto.request.property;

import com.example.smart_booking_system.enums.PropertyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PropertyApplicationSubmitDTO {

    @NotNull(message = "Loại cơ sở không được để trống")
    private PropertyType propertyType;

    @NotBlank(message = "Quốc gia không được để trống")
    private String country;

    @NotBlank(message = "Tỉnh/Thành không được để trống")
    private String province;

    @NotBlank(message = "Thành phố/Quận không được để trống")
    private String city; // Quận/Huyện

    // ✅ [NEW] Thêm các trường code và ward
    private String ward;         // Phường/Xã (Có thể null nếu FE không bắt buộc)
    private String provinceCode;
    private String districtCode;
    private BigDecimal price;         // Giá ngày thường
    private BigDecimal weekendPrice;  // Giá cuối tuần
    private Integer capacity;         // Sức chứa
    private String unitName;          // Tên căn (VD: Villa A)
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Tên cơ sở không được để trống")
    @Size(min = 5, message = "Tên cơ sở phải có ít nhất 5 ký tự")
    private String propertyName;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, message = "Mô tả phải có ít nhất 50 ký tự")
    private String description;

    @NotNull(message = "Diện tích không được để trống")
    @Min(value = 1, message = "Diện tích phải lớn hơn 0")
    private BigDecimal area;

    private Map<String, Boolean> amenities;

    @NotNull(message = "Bạn phải đồng ý với điều khoản")
    private Boolean terms;

    private BigDecimal latitude;
    private BigDecimal longitude;
}