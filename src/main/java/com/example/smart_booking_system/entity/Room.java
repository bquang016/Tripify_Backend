package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roomId; // Khớp với Repository <Room, Integer>

    // ✅ Tên biến là 'property' -> Repository phải dùng 'findByProperty...'
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    @JsonIgnore
    @ToString.Exclude
    private Property property;

    private String roomName;

    // Các trường mới cho Homestay/Villa
    private BigDecimal area;
    private Integer roomAmount;
    private BigDecimal weekendPrice;
    private LocalDate createdDate;

    private BigDecimal pricePerNight;
    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ✅ [FIX] Ánh xạ vào đúng cột 'isActive' trong Database để tránh lỗi insert
    @Column(name = "isActive")
    private boolean active = true;

    // Giữ lại Enum cũ để không lỗi các file khác, nhưng có thể null
    @Enumerated(EnumType.STRING)
    private RoomCategory roomCategory;

    @Enumerated(EnumType.STRING)
    private RoomStatus roomStatus;

    // Quan hệ
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<RoomImage> roomImages;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<RoomAmenity> roomAmenities;

    @OneToMany(mappedBy = "room")
    @JsonIgnore
    private List<Booking> bookings;
}