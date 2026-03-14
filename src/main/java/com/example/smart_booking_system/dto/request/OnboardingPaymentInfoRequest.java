package com.example.smart_booking_system.dto.request;

import lombok.Data;

@Data
public class OnboardingPaymentInfoRequest {
    private String paymentMethod; // "bank" or "card"
    private String bankBin;
    private String bankName;
    private String bankLogo;
    private String accountName;
    private String accountNumber;

    // --- CÁC TRƯỜNG DÀNH CHO STRIPE PAYOUT (THÊM MỚI) ---
    private String stripeToken;
    private String cardLast4;
    private String cardBrand;
}