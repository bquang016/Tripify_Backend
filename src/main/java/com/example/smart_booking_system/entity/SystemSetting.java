package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.ToString
public class SystemSetting {

    @Id
    private Long id = 1L; // We only ever have one row for settings

    @Column(length = 255)
    private String appName = "Tripify";

    @Column(length = 10)
    private String defaultLanguage = "vi";

    @Column(length = 10)
    private String defaultCurrency = "VND";

    @Column(length = 1000)
    private String logoUrl;

    private Boolean isMaintenanceMode = false;

    @Column(length = 1000)
    private String maintenanceMessage;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
