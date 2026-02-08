package com.example.smart_booking_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Corresponds to the 'paymentInfo' object
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingPaymentInfoRequest {
    private String paymentMethod;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
}
