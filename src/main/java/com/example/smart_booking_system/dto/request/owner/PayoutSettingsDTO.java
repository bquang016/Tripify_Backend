package com.example.smart_booking_system.dto.request.owner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutSettingsDTO {
    private String defaultPayoutMethod; // Nhận BANK_TRANSFER, STRIPE, hoặc NONE

    // Thông tin Bank
    private String bankName;
    private String accountHolderName;
    private String accountNumber;

    // Thông tin Stripe
    private String stripeAccountId;
    private String cardLast4;
    private String cardBrand;
    private String stripeToken;
}