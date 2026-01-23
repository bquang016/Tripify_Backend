package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.application.OwnerApplicationSubmitDTO;
import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.enums.ApplicationStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OwnerApplicationService {

    OwnerApplicationDTO submitApplication(OwnerApplicationSubmitDTO submitDTO, String applicantUsername);

    // ===== HÀM BỊ THIẾU MÀ ADMIN CONTROLLER CẦN =====
    @Transactional(readOnly = true)
    List<OwnerApplicationDTO> getPendingOwnerApplications();

    List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status);

    OwnerApplicationDTO reviewApplication(Long applicationId, OwnerApplicationReviewDTO reviewDTO, String adminUsername);
}