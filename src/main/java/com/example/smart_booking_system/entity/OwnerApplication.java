package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registerOwner")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registerOwnerid")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User userId;

    @Column(name = "permanent_address", nullable = false, columnDefinition = "TEXT")
    private String permanentAddress;

    @Column(name = "hometown_address", nullable = false, columnDefinition = "TEXT")
    private String hometownAddress;

    @Column(name = "cardFrontImage", nullable = false, length = 512)
    private String cardFrontImage;

    @Column(name = "cardBackImage", nullable = false, length = 512)
    private String cardBackImage;

    @Column(name = "businessLicenseImage", nullable = false, length = 512)
    private String businessLicenseImage;

    @Column(name = "businessLicenseNumber", nullable = false, length = 255)
    private String businessLicenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "adminReason", columnDefinition = "TEXT")
    private String adminReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedBy;

    @Column(name = "createAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    private LocalDate personalDob;
    private String personalIdCard;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = ApplicationStatus.PENDING;
    }
}