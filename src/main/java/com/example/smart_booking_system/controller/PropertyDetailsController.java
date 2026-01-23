package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.PropertyDetailsResponseDTO;
import com.example.smart_booking_system.service.PropertyDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/property-details")
@RequiredArgsConstructor
public class PropertyDetailsController {

    private final PropertyDetailsService propertyDetailsService;

    @GetMapping("/{propertyId}")
    public ResponseEntity<?> getPropertyDetails(@PathVariable int propertyId) {
        try {
            PropertyDetailsResponseDTO result = propertyDetailsService.getPropertyDetails(propertyId);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(404)
                    .body("Property not found with id: " + propertyId);
        }
    }
}
