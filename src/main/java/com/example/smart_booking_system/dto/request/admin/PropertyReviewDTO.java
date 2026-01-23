package com.example.smart_booking_system.dto.request.admin;

import com.example.smart_booking_system.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PropertyReviewDTO {

    @NotNull(message = "Trạng thái không được để trống")
    private PropertyStatus status;

    private String reason;

    public boolean isValidForReview() {
        return this.status == PropertyStatus.APPROVE
                || this.status == PropertyStatus.REJECTED;
    }
}
