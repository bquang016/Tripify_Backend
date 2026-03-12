package com.example.smart_booking_system.dto.request;

import lombok.Data;

@Data
public class ChargeRequest {
    private Long bookingId;
    private String paymentMethodId; // ID của thẻ Stripe (VD: pm_1TA5U6...)
    private Long amount; // Số tiền (VND)
}