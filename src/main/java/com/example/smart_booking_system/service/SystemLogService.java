package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.SystemLogDetailDTO;
import com.example.smart_booking_system.dto.response.SystemLogResponseDTO;
import com.example.smart_booking_system.entity.SystemLog;
import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import com.example.smart_booking_system.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;


    // GHI LOG (dùng khi CREATE / UPDATE / DELETE)
    public void log(
            com.example.smart_booking_system.entity.User actor,
            LogAction action,
            LogEntityType entityType,
            String entityId,
            String description,
            String oldValue,
            String newValue
    ) {
        SystemLog log = new SystemLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);

        systemLogRepository.save(log);
    }

    //DANH SÁCH LOG (10 / trang)
    public Page<SystemLogResponseDTO> getLogs(int page) {
        return systemLogRepository
                .findAllLogs(PageRequest.of(page, 10))
                .map(this::toResponseDTO);
    }

    public Page<SystemLogResponseDTO> getLogsByEntity(
            LogEntityType entityType,
            String entityId,
            int page
    ) {
        return systemLogRepository
                .findByEntity(entityType, entityId, PageRequest.of(page, 10))
                .map(this::toResponseDTO);
    }

    public Page<SystemLogResponseDTO> getLogsByActor(
            String userId,
            int page
    ) {
        return systemLogRepository
                .findByActor(userId, PageRequest.of(page, 10))
                .map(this::toResponseDTO);
    }

    public Page<SystemLogResponseDTO> getLogsByEntityType(
            LogEntityType entityType,
            int page
    ) {
        return systemLogRepository
                .findByEntityType(entityType, PageRequest.of(page, 10))
                .map(this::toResponseDTO);
    }

    //CHI TIẾT LOG
    public SystemLogDetailDTO getLogDetail(Long logId) {
        SystemLog log = systemLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("System log không tồn tại"));

        return toDetailDTO(log);
    }

    // MAPPING
    private SystemLogResponseDTO toResponseDTO(SystemLog log) {
        return new SystemLogResponseDTO(
                log.getLogId(),
                log.getAction(),
                log.getEntityType(),
                log.getDescription(),
                log.getActor().getEmail(),
                log.getCreatedAt()
        );
    }

    private SystemLogDetailDTO toDetailDTO(SystemLog log) {
        return new SystemLogDetailDTO(
                log.getLogId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getActor().getUserId(),
                log.getActor().getFullName(),
                log.getActor().getEmail(),
                log.getOldValue(),
                log.getNewValue(),
                log.getCreatedAt()
        );
    }
}
