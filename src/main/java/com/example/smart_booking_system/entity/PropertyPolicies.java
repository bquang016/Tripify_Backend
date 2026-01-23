package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name = "property_policies")
public class PropertyPolicies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int policyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId", unique = true)
    private Property propertyId;

    // --- PET POLICY ---
    private boolean petsAllowed;
    @Column(columnDefinition = "TEXT")
    private String petPolicyDescription;

    // --- SMOKING POLICY ---
    private boolean smokingAllowed;
    @Column(columnDefinition = "TEXT")
    private String smokingPolicyDescription;

    // --- CHILDREN POLICY ---
    private boolean childrenAllowed;
    @Column(columnDefinition = "TEXT")
    private String childrenPolicyDescription;

    // --- CHECK-IN / CHECK-OUT POLICY ---
    private LocalTime checkInTime;
    private LocalTime checkOutTime;


    // --- QUIET HOURS ---
    private String quietHours;


    // --- CANCELLATION POLICY (Option A) ---
    private boolean allowFreeCancellation;     // Cho phép hủy miễn phí?
    private Integer freeCancellationDays;      // Cho hủy trước bao nhiêu ngày?
    @Column(columnDefinition = "TEXT")
    private String cancellationPolicyDescription;

    // --- PREPAYMENT POLICY ---
    private boolean requiresPrepayment;
    @Column(columnDefinition = "TEXT")
    private String prepaymentPolicy;

    // --- SECURITY DEPOSIT POLICY ---
    private boolean securityDepositRequired;
    private BigDecimal securityDepositAmount;
    @Column(columnDefinition = "TEXT")
    private String securityDepositDescription;

    // --- AGE RESTRICTION ---
    private Integer minimumAge;
}
