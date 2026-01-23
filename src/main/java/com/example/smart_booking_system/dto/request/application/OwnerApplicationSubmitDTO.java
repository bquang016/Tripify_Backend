package com.example.smart_booking_system.dto.request.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class OwnerApplicationSubmitDTO {

    @NotBlank(message = "Họ tên không được để trống")
    private String personalFullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String personalEmail;

    @NotBlank(message = "SĐT không được để trống")
    private String personalPhone;

    @NotBlank(message = "Số CCCD/Passport không được để trống")
    private String personalIdCard;

    private LocalDate personalDob;

    @NotBlank(message = "Địa chỉ thường trú không được để trống")
    private String permanentAddress;

    @NotBlank(message = "Quê quán không được để trống")
    private String hometownAddress;

    // FE sẽ gửi file MultipartFile (multipart/form-data). Bắt buộc phải có file.
    @NotNull(message = "Ảnh mặt trước CCCD không được để trống")
    private MultipartFile cardFrontImage;

    @NotNull(message = "Ảnh mặt sau CCCD không được để trống")
    private MultipartFile cardBackImage;

    @NotNull(message = "Ảnh giấy phép kinh doanh không được để trống")
    private MultipartFile businessLicenseImage;

    @NotBlank(message = "Số giấy phép kinh doanh không được để trống")
    @Size(min = 5, max = 100)
    private String businessLicenseNumber;
}
