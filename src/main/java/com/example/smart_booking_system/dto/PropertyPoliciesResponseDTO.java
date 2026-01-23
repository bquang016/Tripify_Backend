package com.example.smart_booking_system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class PropertyPoliciesResponseDTO {

    private int policyId;
    private int propertyId;

    private boolean petsAllowed;
    private String petPolicyDescription;

    private boolean smokingAllowed;
    private String smokingPolicyDescription;

    private boolean childrenAllowed;
    private String childrenPolicyDescription;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    private String quietHours;

    private boolean allowFreeCancellation;
    private Integer freeCancellationDays;
    private String cancellationPolicyDescription;

    private boolean requiresPrepayment;
    private String prepaymentPolicy;

    private boolean securityDepositRequired;
    private BigDecimal securityDepositAmount;
    private String securityDepositDescription;

    private Integer minimumAge;
}
