package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "property_details")
public class PropertyDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int propertyDetailId;

    // Quan hệ 1-1 với Property
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId", nullable = false, unique = true)
    private Property property;

    // Diện tích (m2) - Khớp với trường 'area' từ Frontend
    @Column(precision = 10, scale = 2)
    private BigDecimal area;

}