package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.SystemLogDetailDTO;
import com.example.smart_booking_system.dto.response.SystemLogResponseDTO;
import com.example.smart_booking_system.entity.SystemLog;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import com.example.smart_booking_system.repository.SystemLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    // GHI LOG
    public void log(
            User actor,
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

    // 1, 2, 3, 4. TÌM KIẾM, LỌC, PHÂN TRANG, SẮP XẾP ĐỘNG
    public Page<SystemLogResponseDTO> searchLogs(
            String keyword,
            LogAction action,
            LogEntityType entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<SystemLog> spec = buildSpecification(keyword, action, entityType, startDate, endDate);

        return systemLogRepository.findAll(spec, pageable).map(this::toResponseDTO);
    }

    // 5. CHI TIẾT LOG
    public SystemLogDetailDTO getLogDetail(Long logId) {
        SystemLog log = systemLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("System log không tồn tại"));

        return toDetailDTO(log);
    }

    // 6. XUẤT EXCEL
    public byte[] exportLogsToExcel(
            String keyword,
            LogAction action,
            LogEntityType entityType,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) throws IOException {
        Specification<SystemLog> spec = buildSpecification(keyword, action, entityType, startDate, endDate);
        List<SystemLog> logs = systemLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("System Logs");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Hành động", "Loại thực thể", "ID thực thể", "Mô tả", "Người thực hiện", "Email", "Thời gian"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill Data
            int rowIdx = 1;
            for (SystemLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getLogId());
                row.createCell(1).setCellValue(log.getAction().toString());
                row.createCell(2).setCellValue(log.getEntityType().toString());
                row.createCell(3).setCellValue(log.getEntityId());
                row.createCell(4).setCellValue(log.getDescription());
                row.createCell(5).setCellValue(log.getActor().getFullName());
                row.createCell(6).setCellValue(log.getActor().getEmail());
                row.createCell(7).setCellValue(log.getCreatedAt().toString());
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // HÀM BUILD SPECIFICATION (DÙNG CHUNG CHO SEARCH VÀ EXPORT)
    private Specification<SystemLog> buildSpecification(
            String keyword,
            LogAction action,
            LogEntityType entityType,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), likeKeyword),
                        cb.like(cb.lower(root.get("entityId")), likeKeyword),
                        cb.like(cb.lower(root.get("actor").get("email")), likeKeyword),
                        cb.like(cb.lower(root.get("actor").get("fullName")), likeKeyword)
                ));
            }

            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // GIỮ LẠI CÁC HÀM CŨ ĐỂ KHÔNG LÀM BREAK CODE HIỆN TẠI (NẾU CẦN)
    public Page<SystemLogResponseDTO> getLogs(int page) {
        return searchLogs(null, null, null, null, null, page, 10, "createdAt", "desc");
    }

    public Page<SystemLogResponseDTO> getLogsByEntity(LogEntityType entityType, String entityId, int page) {
        return searchLogs(entityId, null, entityType, null, null, page, 10, "createdAt", "desc");
    }

    public Page<SystemLogResponseDTO> getLogsByActor(String userId, int page) {
        // Vì searchLogs dùng keyword chung, nếu muốn tìm chính xác userId thì có thể tạo spec riêng hoặc dùng keyword
        return searchLogs(userId, null, null, null, null, page, 10, "createdAt", "desc");
    }

    public Page<SystemLogResponseDTO> getLogsByEntityType(LogEntityType entityType, int page) {
        return searchLogs(null, null, entityType, null, null, page, 10, "createdAt", "desc");
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
