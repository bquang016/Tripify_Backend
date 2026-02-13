package com.example.smart_booking_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Corresponds to the root of the JSON 'data' part
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FullOnboardingRequest {
    // --- Step 1: Personal Info ---
    private String fullName;
    private String phoneNumber;
    private String identityCardNumber;
    private String dateOfBirth; // Keep as String for flexibility
    private String gender;
    private String address;
    private String city;

    // --- Step 2: Property Info ---
    private OnboardingPropertyInfoRequest propertyInfo;

    // --- Step 3: Payment Info ---
    private OnboardingPaymentInfoRequest paymentInfo;
}
