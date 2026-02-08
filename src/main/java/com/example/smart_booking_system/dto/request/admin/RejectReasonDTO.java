package com.example.smart_booking_system.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectReasonDTO {
    @NotBlank(message = "Rejection reason is required.")
    private String reason;
}
