package com.example.smart_booking_system.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendRequestDTO {
    @NotBlank(message = "Lý do không được để trống")
    private String reason;
}