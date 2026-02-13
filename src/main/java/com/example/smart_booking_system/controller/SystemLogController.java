package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.SystemLogDetailDTO;
import com.example.smart_booking_system.dto.response.SystemLogResponseDTO;
import com.example.smart_booking_system.enums.LogEntityType;
import com.example.smart_booking_system.service.SystemLogService;
import com.example.smart_booking_system.security.CheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system-logs")
@RequiredArgsConstructor
@CheckPermission("SYSTEM_LOG_VIEW")
public class SystemLogController {

    private final SystemLogService systemLogService;

    // FE TABLE – Xem toàn bộ log (10 / trang)
    @GetMapping
    public Page<SystemLogResponseDTO> getAllLogs(
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogs(page);
    }

    // FE TABLE – Xem log theo entity (PROPERTY / ROOM)
    @GetMapping("/entity")
    public Page<SystemLogResponseDTO> getLogsByEntity(
            @RequestParam LogEntityType entityType,
            @RequestParam String entityId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogsByEntity(entityType, entityId, page);
    }

    // FE TABLE – Xem log theo người thực hiện
    @GetMapping("/actor/{userId}")
    public Page<SystemLogResponseDTO> getLogsByActor(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogsByActor(userId, page);
    }

    // FE TABLE – Xem log theo loại entity
    @GetMapping("/type/{entityType}")
    public Page<SystemLogResponseDTO> getLogsByEntityType(
            @PathVariable LogEntityType entityType,
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogsByEntityType(entityType, page);
    }

    // FE MODAL – Xem chi tiết 1 log
    @GetMapping("/{logId}")
    public SystemLogDetailDTO getLogDetail(
            @PathVariable Long logId
    ) {
        return systemLogService.getLogDetail(logId);
    }
}
