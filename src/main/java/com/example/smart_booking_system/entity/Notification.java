package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; // Dùng annotation này tiện hơn

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private NotificationType type;

    // Quan trọng: Đặt mặc định là false
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // Liên kết ID của đối tượng liên quan (VD: ID Booking, ID Property...)
    // Để khi user click vào thông báo sẽ điều hướng đúng chỗ
    private String relatedEntityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}