package com.example.smart_booking_system.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AddMultipleAmenityRequest {
    private List<Integer> amenityIds;
}
