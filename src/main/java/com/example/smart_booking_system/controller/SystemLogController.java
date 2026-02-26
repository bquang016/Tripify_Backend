package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.SystemLogDetailDTO;
import com.example.smart_booking_system.dto.response.SystemLogResponseDTO;
import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import com.example.smart_booking_system.service.SystemLogService;
import com.example.smart_booking_system.security.CheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/system-logs")
@RequiredArgsConstructor
@CheckPermission("SYSTEM_LOG_VIEW")
public class SystemLogController {

    private final SystemLogService systemLogService;

    // 1, 2, 3, 4. API TỔNG HỢP: SEARCH, FILTER, PAGINATION, SORT
    @GetMapping("/search")
    public Page<SystemLogResponseDTO> searchLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LogAction action,
            @RequestParam(required = false) LogEntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return systemLogService.searchLogs(keyword, action, entityType, startDate, endDate, page, size, sortBy, sortDir);
    }

    // 5. CHI TIẾT LOG
    @GetMapping("/{logId}")
    public SystemLogDetailDTO getLogDetail(@PathVariable Long logId) {
        return systemLogService.getLogDetail(logId);
    }

    // 6. XUẤT EXCEL
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LogAction action,
            @RequestParam(required = false) LogEntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) throws IOException {
        byte[] data = systemLogService.exportLogsToExcel(keyword, action, entityType, startDate, endDate);

        String filename = "system_logs_" + LocalDateTime.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    // --- CÁC API CŨ (Giữ lại để tương thích hoặc có thể xóa nếu FE đã chuyển sang /search) ---

    @GetMapping
    public Page<SystemLogResponseDTO> getAllLogs(@RequestParam(defaultValue = "0") int page) {
        return systemLogService.getLogs(page);
    }

    @GetMapping("/entity")
    public Page<SystemLogResponseDTO> getLogsByEntity(
            @RequestParam LogEntityType entityType,
            @RequestParam String entityId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogsByEntity(entityType, entityId, page);
    }

    @GetMapping("/actor/{userId}")
    public Page<SystemLogResponseDTO> getLogsByActor(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogsByActor(userId, page);
    }

    @GetMapping("/type/{entityType}")
    public Page<SystemLogResponseDTO> getLogsByEntityType(
            @PathVariable LogEntityType entityType,
            @RequestParam(defaultValue = "0") int page
    ) {
        return systemLogService.getLogsByEntityType(entityType, page);
    }
}
