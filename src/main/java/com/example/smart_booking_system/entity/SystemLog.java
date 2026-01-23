package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    // ai hành động
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User actor;

    // hành động gì
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogAction action;

    // tác động tới cái gì
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LogEntityType entityType;

    // id của cái bị tác động
    @Column(nullable = false)
    private String entityId;

    // mô tả
    @Column(length = 500)
    private String description;

    // JSON lưu trước & sau khi thay đổi
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
