package com.example.smart_booking_system.dto.request.owner;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OwnerProfileRequest {
    private String fullName;
    private String phoneNumber;
    private String identityCardNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String country;
    private String profilePhotoUrl; // Nếu upload ảnh xong trả về URL
}