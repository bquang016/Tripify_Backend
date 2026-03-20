package com.example.smart_booking_system.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawalSubmitRequest {
    private BigDecimal amount;
}