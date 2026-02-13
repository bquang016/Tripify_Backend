package com.example.smart_booking_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// This DTO can be reused as its structure matches the new 'policies' object.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyPolicyRequest {
    private String checkInTime;
    private String checkOutTime;
    private Integer minimumAge;
    private boolean allowFreeCancellation;
    private Integer freeCancellationDays;
    // Note: The new spec doesn't include all old policy fields,
    // but we can keep them in the DTO for future use.
    // They will just be null if not provided in the JSON.
    private boolean smokingAllowed;
    private boolean petsAllowed;
    private boolean childrenAllowed;
    private boolean securityDepositRequired;
    private BigDecimal securityDepositAmount;
}
