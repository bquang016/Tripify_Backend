package com.example.smart_booking_system.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOwnerOtpResponse {
    private String temporaryToken;
}
