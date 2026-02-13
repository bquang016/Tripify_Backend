package com.example.smart_booking_system.dto.internal;

import com.example.smart_booking_system.dto.request.OwnerApplicationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTO used to store the complete application data, including generated image URLs,
 * as a JSON blob in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplicationData {
    // User Profile Info
    private String fullName;
    private String phoneNumber;
    private String identityCardNumber;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String avatarUrl;
    private String cccdFrontUrl;
    private String cccdBackUrl;

    // Property Info
    private OwnerApplicationRequest.PropertyInfo propertyInfo;
    private String businessLicenseImage;
    private List<String> propertyImageUrls;
    private List<String> unitImageUrls;

    // Payment Info
    private OwnerApplicationRequest.PaymentInfo paymentInfo;
}
