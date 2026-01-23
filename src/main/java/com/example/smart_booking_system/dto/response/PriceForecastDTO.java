package com.example.smart_booking_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceForecastDTO {
    private LocalDate date;
    private String dayOfWeek;
    private BigDecimal price;
    private boolean isWeekend;
}