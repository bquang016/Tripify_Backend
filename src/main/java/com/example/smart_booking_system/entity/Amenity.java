package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.AmenityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Amenity")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int amenityId;

    private String amenityName;

    @Enumerated(EnumType.STRING)
    private AmenityType amenityType;

    private boolean isActive = true;
}
