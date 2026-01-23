package com.example.smart_booking_system.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequestDTO {
    @NotBlank(message = "nội dung không được để trống")
    private String message;
}
