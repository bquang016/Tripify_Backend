package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.MembershipRank; // ✅ Import mới
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "userdetail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userdetailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(length = 50)
    private String gender;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 512)
    private String profilePhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    // ✅ THÊM CÁC TRƯỜNG BỊ THIẾU ĐỂ FIX LỖI DTO
    @Column(columnDefinition = "integer default 0")
    private int points = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_rank")
    private MembershipRank membershipRank;

    @Column(name = "notification_email")
    private String notificationEmail;
    // ------------------------------------------

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}