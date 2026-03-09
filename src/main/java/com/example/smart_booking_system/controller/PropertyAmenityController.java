package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import com.example.smart_booking_system.service.PropertyAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/propertyAmenity")
@RequiredArgsConstructor
public class PropertyAmenityController {

    private final PropertyAmenityService propertyAmenityService;


    // -------------------------------------------------------------------
    // ADD MULTIPLE
    // -------------------------------------------------------------------
    @PostMapping("/add-multiple")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> addMultiple(
            @RequestParam int propertyId,
            @RequestBody Map<String, List<Integer>> body
    ) {
        try {
            List<Integer> amenityIds = body.get("amenityIds");
            List<PropertyAmenityResponseDTO> result =
                    propertyAmenityService.addMultiplePropertyAmenity(propertyId, amenityIds);

            return ResponseEntity.status(201).body(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // -------------------------------------------------------------------
    // READ ALL FOR PROPERTY
    // -------------------------------------------------------------------
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getByProperty(@PathVariable int propertyId) {
        try {
            return ResponseEntity.ok(propertyAmenityService.getByPropertyId(propertyId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // -------------------------------------------------------------------
    // UPDATE BY OLD-AMENITY-ID
    // -------------------------------------------------------------------
    @PutMapping("/update/property/{propertyId}/amenity/{oldAmenityId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateAmenity(
            @PathVariable int propertyId,
            @PathVariable int oldAmenityId,
            @RequestBody Map<String, Integer> body
    ) {
        try {
            int newAmenityId = body.get("newAmenityId");
            PropertyAmenityResponseDTO updated =
                    propertyAmenityService.updatePropertyAmenity(propertyId, oldAmenityId, newAmenityId);

            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // -------------------------------------------------------------------
    // DELETE AMENITY FROM PROPERTY
    // -------------------------------------------------------------------
    @DeleteMapping("/delete/property/{propertyId}/amenity/{amenityId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteAmenity(
            @PathVariable int propertyId,
            @PathVariable int amenityId
    ) {
        try {
            propertyAmenityService.deletePropertyAmenity(propertyId, amenityId);
            return ResponseEntity.ok("Property amenity removed successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
