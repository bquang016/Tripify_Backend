package com.example.smart_booking_system.entity;


import com.example.smart_booking_system.enums.RatingType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "rating")
public class    Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ratingId;

    @JoinColumn(name = "users_id")
    @ManyToOne
    private User userId;

    @JoinColumn(name = "booking_id")
    @ManyToOne
    private Booking bookingId;

    @Column(precision = 2, scale = 1)
    private int rating;

    @Column(nullable = true)
    private String comment;

    private RatingType ratingType;

    @Column(name = "isHidden")
    private boolean isHidden;

    @OneToMany(mappedBy = "rating", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RatingImage> images;

    private LocalDateTime createdAt;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;
}
