package com.example.smart_booking_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.smart_booking_system.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString; // ✅ Import mới
import lombok.EqualsAndHashCode; // ✅ Import mới

@Entity
@Table(name = "social_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider; // GOOGLE, FACEBOOK

    @Column(nullable = false)
    private String providerId; // ID từ Google/Facebook

    @Column(nullable = false)
    private String email; // Email của tài khoản MXH

    private String name; // Tên trên MXH

    // ✅ SỬA: Ngắt vòng lặp toString với User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
}