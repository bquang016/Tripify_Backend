package com.example.smart_booking_system.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class RatingRequestDTO {
    private int bookingId;
    private int stars;    // Số sao (1-5)
    private String comment;
    private LocalDate createdAt;
}