package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.application.OwnerApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.service.OwnerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class OwnerApplicationController {

    private final OwnerApplicationService applicationService;

    @PostMapping(
            value = "/submit-owner",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OwnerApplicationDTO> submitApplication(
            @Valid @ModelAttribute OwnerApplicationSubmitDTO submitDTO,
            Authentication authentication) {

        String username = authentication.getName();

        OwnerApplicationDTO submittedApp =
                applicationService.submitApplication(submitDTO, username);

        return ResponseEntity.ok(submittedApp);
    }
}
