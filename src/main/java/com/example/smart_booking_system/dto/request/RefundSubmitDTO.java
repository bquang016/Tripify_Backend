package com.example.smart_booking_system.dto.request;
import lombok.Data;

@Data
public class RefundSubmitDTO {
    private String reason;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}