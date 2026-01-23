package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "PropertyAmenity")
public class PropertyAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int propertyAmenityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenityId")
    private Amenity amenity;

    private boolean active = true;
}
