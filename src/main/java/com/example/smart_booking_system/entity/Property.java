package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int propertyId;

    // ✅ GIỮ LẠI CÁI NÀY (MappedBy = "property" là đúng với Room.java mới)
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL)
    private List<Room> rooms;

    private String propertyName;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User owner;

    private String address;
    private String country;

    // ========================================================
    // ĐỊA LÝ
    // ========================================================
    @Column(name = "city")
    private String city;      // LƯU TỈNH/THÀNH PHỐ

    @Column(name = "district")
    private String district;  // LƯU QUẬN/HUYỆN

    private String province;
    private String ward;
    private String provinceCode;
    private String districtCode;
    private String postalCode;

    // ========================================================

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    private String phoneContact;
    private String emailContact;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "star_rating")
    private int starRating = 0;

    private int reviewCount = 0;

    private boolean isActive = false;

    @Enumerated(EnumType.STRING)
    private PropertyStatus propertyStatus;

    private String businessLicenseNumber;
    private String businessLicenseImage;

    private LocalDate createdAt = LocalDate.now();
    private LocalDate updatedAt = LocalDate.now();

    // RELATIONS

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PropertyAmenity> propertyAmenities;


    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PropertyImage> images;

    @OneToOne(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PropertyDetail propertyDetail;
}