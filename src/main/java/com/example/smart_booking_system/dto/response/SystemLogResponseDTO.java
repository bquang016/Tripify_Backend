package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SystemLogResponseDTO {

    private Long logId;
    private LogAction action;
    private LogEntityType entityType;

    // description NGẮN
    private String description;

    private String actorEmail;
    private LocalDateTime createdAt;
}
