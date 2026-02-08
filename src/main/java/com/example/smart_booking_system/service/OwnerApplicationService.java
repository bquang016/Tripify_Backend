package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.enums.ApplicationStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OwnerApplicationService {
    List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status);

    void approveApplication(Long applicationId, String adminUsername);

    void rejectApplication(Long applicationId, String reason, String adminUsername);
}