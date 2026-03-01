package com.example.smart_booking_system.dto.request;

import lombok.Data;

@Data
public class MaintenanceModeUpdateDTO {
    private Boolean isEnabled;
    private String message;
}
