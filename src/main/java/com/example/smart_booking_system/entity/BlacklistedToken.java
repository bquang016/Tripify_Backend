package com.example.smart_booking_system.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "BlackListedTokens")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class BlacklistedToken {

    @Id
    @Column(length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}