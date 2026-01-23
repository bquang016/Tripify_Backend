package com.example.smart_booking_system.dto.request.admin;

import com.example.smart_booking_system.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OwnerApplicationReviewDTO {

    @NotNull(message = "Trạng thái không được để trống")
    private ApplicationStatus status;

    private String reason;

    public boolean isValidForReview() {
        return this.status == ApplicationStatus.APPROVED
                || this.status == ApplicationStatus.REJECTED;
    }
}
