package com.example.smart_booking_system.dto.request;

import lombok.Data;

@Data
public class ExchangeRateUpdateDTO {
    private String pair;
    private Double rate;
}
