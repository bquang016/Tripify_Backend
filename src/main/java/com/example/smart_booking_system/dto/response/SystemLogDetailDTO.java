package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SystemLogDetailDTO {

    private Long logId;

    private LogAction action;
    private LogEntityType entityType;
    private String entityId;

    private String description;

    private String actorId;
    private String actorName;
    private String actorEmail;

    private String oldValue;
    private String newValue;

    private LocalDateTime createdAt;
}
